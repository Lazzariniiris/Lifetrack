package com.lifetrack.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

@Serializable data class AuthCredentials(val email: String, val password: String)
@Serializable data class RefreshRequest(@SerialName("refresh_token") val refreshToken: String)
@Serializable data class RecoveryRequest(val email: String)
@Serializable data class SupabaseUser(val id: String, val email: String? = null)
@Serializable data class AuthSessionDto(
    @SerialName("access_token") val accessToken: String = "",
    @SerialName("refresh_token") val refreshToken: String = "",
    @SerialName("expires_in") val expiresIn: Long = 0,
    val user: SupabaseUser,
)

interface AuthApi {
    @POST("auth/v1/signup") suspend fun register(@Header("apikey") apiKey: String, @Body body: AuthCredentials): AuthSessionDto
    @POST("auth/v1/token") suspend fun login(@Header("apikey") apiKey: String, @Query("grant_type") grantType: String = "password", @Body body: AuthCredentials): AuthSessionDto
    @POST("auth/v1/token") suspend fun refresh(@Header("apikey") apiKey: String, @Query("grant_type") grantType: String = "refresh_token", @Body body: RefreshRequest): AuthSessionDto
    @POST("auth/v1/recover") suspend fun recover(@Header("apikey") apiKey: String, @Body body: RecoveryRequest)
    @POST("auth/v1/logout") suspend fun logout(@Header("apikey") apiKey: String, @Header("Authorization") authorization: String)
}
