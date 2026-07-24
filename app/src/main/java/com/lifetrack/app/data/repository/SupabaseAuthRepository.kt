package com.lifetrack.app.data.repository

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lifetrack.app.BuildConfig
import com.lifetrack.app.data.remote.AuthApi
import com.lifetrack.app.data.remote.AuthCredentials
import com.lifetrack.app.data.remote.AuthSessionDto
import com.lifetrack.app.data.remote.PasswordUpdateRequest
import com.lifetrack.app.data.remote.RecoveryRequest
import com.lifetrack.app.data.remote.RefreshRequest
import com.lifetrack.app.data.remote.SupabaseErrorDto
import com.lifetrack.app.data.remote.SupabaseUser
import com.lifetrack.app.domain.repository.AuthError
import com.lifetrack.app.domain.repository.AuthFailure
import com.lifetrack.app.domain.repository.AuthRecoveryTokens
import com.lifetrack.app.domain.repository.AuthRepository
import com.lifetrack.app.domain.repository.AuthResult
import com.lifetrack.app.domain.repository.AuthUser
import com.lifetrack.app.domain.repository.RegistrationOutcome
import com.lifetrack.app.domain.repository.normalizeAuthEmail
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.net.SocketTimeoutException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import retrofit2.HttpException

private val Context.sessionDataStore by preferencesDataStore("secure_session")
private const val RECOVERY_REDIRECT = "lifetrack://auth/recovery"

@Singleton
class SupabaseAuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: AuthApi,
    private val json: Json,
) : AuthRepository {
    override val configured = BuildConfig.SUPABASE_URL.startsWith("https://") &&
        BuildConfig.SUPABASE_ANON_KEY.isNotBlank()
    override val user: Flow<AuthUser?> = context.sessionDataStore.data.map(::storedUser)
    private val refreshMutex = Mutex()

    override suspend fun register(email: String, password: String): AuthResult<RegistrationOutcome> =
        execute(AuthOperation.Register) {
            val normalizedEmail = normalizeAuthEmail(email)
            val session = api.register(key(), AuthCredentials(normalizedEmail, password))
            if (session.accessToken.isNullOrBlank()) {
                RegistrationOutcome.VerificationPending(normalizedEmail)
            } else {
                check(save(session))
                RegistrationOutcome.SignedIn
            }
        }

    override suspend fun login(email: String, password: String): AuthResult<Unit> = execute(AuthOperation.Login) {
        check(save(api.login(key(), body = AuthCredentials(normalizeAuthEmail(email), password))))
    }

    override suspend fun recover(email: String): AuthResult<Unit> = execute(AuthOperation.Recovery) {
        api.recover(key(), RECOVERY_REDIRECT, RecoveryRequest(normalizeAuthEmail(email)))
    }

    override suspend fun completePasswordRecovery(
        tokens: AuthRecoveryTokens,
        password: String,
    ): AuthResult<Unit> = execute(AuthOperation.Recovery) {
        val user = api.updateUser(key(), "Bearer ${tokens.accessToken}", PasswordUpdateRequest(password))
        check(
            save(
                AuthSessionDto(
                    accessToken = tokens.accessToken,
                    refreshToken = tokens.refreshToken,
                    expiresIn = tokens.expiresInSeconds,
                    user = user,
                ),
            ),
        )
    }

    override suspend fun logout(): AuthResult<Unit> {
        if (configured) {
            validAccessToken()?.let { token -> runCatching { api.logout(key(), "Bearer $token") } }
        }
        clear()
        return AuthResult.Success(Unit)
    }

    override suspend fun validateSession(): AuthResult<AuthUser?> {
        if (!configured) return failure(AuthError.NotConfigured)
        return when (val token = currentOrRefreshedAccessToken()) {
            is AuthResult.Failure -> token
            is AuthResult.Success -> {
                if (token.value == null) AuthResult.Success(null)
                else {
                    val user = storedUser(context.sessionDataStore.data.first())
                    if (user == null) failure(AuthError.Unknown) else AuthResult.Success(user)
                }
            }
        }
    }

    override suspend fun validAccessToken(): String? {
        if (!configured) return null
        return when (val result = currentOrRefreshedAccessToken()) {
            is AuthResult.Success -> result.value
            is AuthResult.Failure -> null
        }
    }

    private suspend fun currentOrRefreshedAccessToken(): AuthResult<String?> = refreshMutex.withLock {
        val values = context.sessionDataStore.data.first()
        val accessToken = values[ACCESS_TOKEN]?.let(SessionCipher::decrypt) ?: return@withLock AuthResult.Success(null)
        if ((values[EXPIRES_AT] ?: 0) > System.currentTimeMillis() + 60_000) {
            return@withLock AuthResult.Success(accessToken)
        }
        val refreshToken = values[REFRESH_TOKEN]?.let(SessionCipher::decrypt) ?: return@withLock failure(AuthError.SessionExpired)
        try {
            val refreshed = api.refresh(key(), body = RefreshRequest(refreshToken))
            val fallbackUser = storedUser(values)?.let { SupabaseUser(it.id, it.email) }
            if (!save(refreshed, refreshToken, fallbackUser)) return@withLock failure(AuthError.Unknown)
            AuthResult.Success(refreshed.accessToken)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: HttpException) {
            if (error.code() == 400 || error.code() == 401) {
                clear()
                failure(AuthError.SessionExpired)
            } else {
                failure(mapAuthError(error, AuthOperation.Refresh))
            }
        } catch (_: SocketTimeoutException) {
            failure(AuthError.Timeout)
        } catch (_: IOException) {
            failure(AuthError.Network)
        } catch (_: Throwable) {
            failure(AuthError.Unknown)
        }
    }

    private fun key(): String = BuildConfig.SUPABASE_ANON_KEY

    private suspend fun save(
        session: AuthSessionDto,
        fallbackRefreshToken: String? = null,
        fallbackUser: SupabaseUser? = null,
    ): Boolean {
        val accessToken = session.accessToken?.takeIf(String::isNotBlank) ?: return false
        val refreshToken = session.refreshToken?.takeIf(String::isNotBlank) ?: fallbackRefreshToken ?: return false
        val user = session.user ?: fallbackUser ?: return false
        val encryptedAccessToken = SessionCipher.encrypt(accessToken) ?: return false
        val encryptedRefreshToken = SessionCipher.encrypt(refreshToken) ?: return false
        context.sessionDataStore.edit { values ->
            values[ACCESS_TOKEN] = encryptedAccessToken
            values[REFRESH_TOKEN] = encryptedRefreshToken
            values[EXPIRES_AT] = System.currentTimeMillis() + session.expiresIn.coerceAtLeast(0) * 1_000
            values[USER_ID] = user.id
            values[EMAIL] = user.email.orEmpty()
        }
        return true
    }

    private suspend fun clear() = context.sessionDataStore.edit { it.clear() }

    private suspend fun <T> execute(operation: AuthOperation, block: suspend () -> T): AuthResult<T> {
        if (!configured) return failure(AuthError.NotConfigured)
        return try {
            AuthResult.Success(block())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: HttpException) {
            failure(mapAuthError(error, operation))
        } catch (_: SocketTimeoutException) {
            failure(AuthError.Timeout)
        } catch (_: IOException) {
            failure(AuthError.Network)
        } catch (_: Throwable) {
            failure(AuthError.Unknown)
        }
    }

    private fun mapAuthError(error: HttpException, operation: AuthOperation): AuthError {
        val payload = runCatching {
            error.response()?.errorBody()?.string()?.let { json.decodeFromString<SupabaseErrorDto>(it) }
        }.getOrNull()
        return mapAuthError(
            statusCode = error.code(),
            errorCode = payload?.errorCode ?: payload?.code,
            serverMessage = payload?.message ?: payload?.msg ?: payload?.errorDescription,
            operation = operation,
        )
    }

    private companion object {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val EXPIRES_AT = longPreferencesKey("expires_at")
        val USER_ID = stringPreferencesKey("user_id")
        val EMAIL = stringPreferencesKey("email")
    }
}

private fun storedUser(values: Preferences): AuthUser? {
    val id = values[stringPreferencesKey("user_id")] ?: return null
    return AuthUser(id, values[stringPreferencesKey("email")].orEmpty())
}

internal enum class AuthOperation { Register, Login, Recovery, Refresh }

internal fun mapAuthError(
    statusCode: Int?,
    errorCode: String?,
    serverMessage: String?,
    operation: AuthOperation,
): AuthError {
    val code = errorCode.orEmpty().lowercase()
    val message = serverMessage.orEmpty().lowercase()
    return when {
        statusCode == 429 -> AuthError.RateLimited
        statusCode == 408 -> AuthError.Timeout
        code in setOf("email_not_confirmed", "email_not_verified") ||
            "email not confirmed" in message -> AuthError.EmailNotVerified
        code in setOf("user_already_exists", "email_exists") ||
            "already registered" in message -> AuthError.EmailAlreadyRegistered
        code in setOf("invalid_credentials", "invalid_grant") ||
            "invalid login credentials" in message -> AuthError.InvalidCredentials
        operation == AuthOperation.Login && statusCode in setOf(400, 401) -> AuthError.InvalidCredentials
        operation == AuthOperation.Recovery && code in setOf("otp_expired", "bad_jwt", "invalid_token") ->
            AuthError.RecoveryLinkInvalid
        else -> AuthError.Unknown
    }
}

private fun failure(error: AuthError) = AuthResult.Failure(AuthFailure(error))

private object SessionCipher {
    private const val KEY_ALIAS = "lifetrack_session_v1"
    private const val PREFIX = "enc:"

    fun encrypt(value: String): String? = runCatching {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val payload = cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        PREFIX + Base64.encodeToString(payload, Base64.NO_WRAP)
    }.getOrNull()

    fun decrypt(value: String): String? {
        if (!value.startsWith(PREFIX)) return value
        return runCatching {
            val payload = Base64.decode(value.removePrefix(PREFIX), Base64.NO_WRAP)
            if (payload.size <= 12) return null
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, payload.copyOfRange(0, 12)))
            cipher.doFinal(payload.copyOfRange(12, payload.size)).toString(Charsets.UTF_8)
        }.getOrNull()
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
            generateKey()
        }
    }
}
