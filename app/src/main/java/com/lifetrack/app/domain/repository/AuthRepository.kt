package com.lifetrack.app.domain.repository

import com.lifetrack.app.domain.model.AppResult
import kotlinx.coroutines.flow.Flow

data class AuthUser(val id: String, val email: String)
interface AuthRepository {
    val user: Flow<AuthUser?>
    val configured: Boolean
    suspend fun register(email: String, password: String): AppResult<Unit>
    suspend fun login(email: String, password: String): AppResult<Unit>
    suspend fun logout(): AppResult<Unit>
    suspend fun recover(email: String): AppResult<Unit>
    suspend fun validAccessToken(): String?
}
