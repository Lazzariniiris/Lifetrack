package com.lifetrack.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

@Serializable data class AuthCredentials(val email: String, val password: String)
@Serializable data class RefreshRequest(@SerialName("refresh_token") val refreshToken: String)
@Serializable data class RecoveryRequest(val email: String)
@Serializable data class PasswordUpdateRequest(val password: String)
@Serializable data class SupabaseUser(val id: String, val email: String? = null)
@Serializable data class AuthSessionDto(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long = 0,
    val user: SupabaseUser? = null,
)
@Serializable data class SupabaseErrorDto(
    val code: String? = null,
    @SerialName("error_code") val errorCode: String? = null,
    val msg: String? = null,
    val message: String? = null,
    @SerialName("error_description") val errorDescription: String? = null,
)

interface AuthApi {
    @POST("auth/v1/signup") suspend fun register(@Header("apikey") apiKey: String, @Body body: AuthCredentials): AuthSessionDto
    @POST("auth/v1/token") suspend fun login(@Header("apikey") apiKey: String, @Query("grant_type") grantType: String = "password", @Body body: AuthCredentials): AuthSessionDto
    @POST("auth/v1/token") suspend fun refresh(@Header("apikey") apiKey: String, @Query("grant_type") grantType: String = "refresh_token", @Body body: RefreshRequest): AuthSessionDto
    @POST("auth/v1/recover") suspend fun recover(
        @Header("apikey") apiKey: String,
        @Query("redirect_to") redirectTo: String,
        @Body body: RecoveryRequest,
    )
    @PUT("auth/v1/user") suspend fun updateUser(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body body: PasswordUpdateRequest,
    ): SupabaseUser
    @POST("auth/v1/logout") suspend fun logout(@Header("apikey") apiKey: String, @Header("Authorization") authorization: String)
}
