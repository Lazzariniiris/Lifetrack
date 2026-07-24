package com.lifetrack.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.Query

@Serializable
data class UserProfileDto(
    val id: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("health_goal") val healthGoal: String? = null,
    @SerialName("weight_kg") val weightKg: Double? = null,
    @SerialName("height_cm") val heightCm: Double? = null,
    @SerialName("activity_level") val activityLevel: String? = null,
    @SerialName("daily_calorie_goal") val dailyCalorieGoal: Int? = null,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class ProfileUpdateDto(
    @SerialName("display_name") val displayName: String,
    @SerialName("health_goal") val healthGoal: String?,
    @SerialName("weight_kg") val weightKg: Double?,
    @SerialName("height_cm") val heightCm: Double?,
    @SerialName("activity_level") val activityLevel: String?,
    @SerialName("daily_calorie_goal") val dailyCalorieGoal: Int?,
)

interface ProfileApi {
    @GET("rest/v1/profiles")
    suspend fun getProfile(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("select") select: String = "id,display_name,health_goal,weight_kg,height_cm,activity_level,daily_calorie_goal,created_at",
        @Query("id") id: String,
        @Query("limit") limit: Int = 1,
    ): List<UserProfileDto>

    @PATCH("rest/v1/profiles")
    suspend fun updateProfile(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Query("id") id: String,
        @Body update: ProfileUpdateDto,
    ): List<UserProfileDto>
}
