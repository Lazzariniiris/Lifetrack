package com.lifetrack.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lifetrack.app.BuildConfig
import com.lifetrack.app.data.remote.AuthApi
import com.lifetrack.app.data.remote.AuthCredentials
import com.lifetrack.app.data.remote.AuthSessionDto
import com.lifetrack.app.data.remote.RecoveryRequest
import com.lifetrack.app.data.remote.RefreshRequest
import com.lifetrack.app.domain.model.AppResult
import com.lifetrack.app.domain.repository.AuthRepository
import com.lifetrack.app.domain.repository.AuthUser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionDataStore by preferencesDataStore("secure_session")

@Singleton
class SupabaseAuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: AuthApi,
) : AuthRepository {
    override val configured = BuildConfig.SUPABASE_URL.startsWith("https://") && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()
    override val user: Flow<AuthUser?> = context.sessionDataStore.data.map { values ->
        val id = values[USER_ID] ?: return@map null
        AuthUser(id, values[EMAIL].orEmpty())
    }

    override suspend fun register(email: String, password: String) = request {
        require(password.length >= 8) { "La contrasena debe tener al menos 8 caracteres." }
        api.register(key(), AuthCredentials(email.trim(), password)).takeIf { it.accessToken.isNotBlank() }?.let { save(it) }
    }
    override suspend fun login(email: String, password: String) = request { save(api.login(key(), body = AuthCredentials(email.trim(), password))) }
    override suspend fun recover(email: String) = request { api.recover(key(), RecoveryRequest(email.trim())) }
    override suspend fun logout(): AppResult<Unit> {
        val token = validAccessToken()
        if (token != null) runCatching { api.logout(key(), "Bearer $token") }
        clear()
        return AppResult.Success(Unit)
    }
    override suspend fun validAccessToken(): String? {
        val values = context.sessionDataStore.data.first()
        val accessToken = values[ACCESS_TOKEN] ?: return null
        if ((values[EXPIRES_AT] ?: 0) > System.currentTimeMillis() + 60_000) return accessToken
        val refreshToken = values[REFRESH_TOKEN] ?: return null
        return runCatching { api.refresh(key(), body = RefreshRequest(refreshToken)).also { save(it) }.accessToken }.getOrElse { clear(); null }
    }

    private fun key() = BuildConfig.SUPABASE_ANON_KEY.ifBlank { error("Supabase no esta configurado") }
    private suspend fun save(session: AuthSessionDto) = context.sessionDataStore.edit { values ->
        values[ACCESS_TOKEN] = session.accessToken; values[REFRESH_TOKEN] = session.refreshToken
        values[EXPIRES_AT] = System.currentTimeMillis() + session.expiresIn * 1_000
        values[USER_ID] = session.user.id; values[EMAIL] = session.user.email.orEmpty()
    }
    private suspend fun clear() = context.sessionDataStore.edit { it.clear() }
    private suspend fun request(block: suspend () -> Unit): AppResult<Unit> = runCatching { block() }.fold({ AppResult.Success(Unit) }, { AppResult.Error(it.message ?: "No se pudo completar la solicitud") })
    private companion object {
        val ACCESS_TOKEN = stringPreferencesKey("access_token"); val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val EXPIRES_AT = longPreferencesKey("expires_at"); val USER_ID = stringPreferencesKey("user_id"); val EMAIL = stringPreferencesKey("email")
    }
}
