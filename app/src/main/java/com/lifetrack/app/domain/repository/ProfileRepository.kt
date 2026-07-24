package com.lifetrack.app.domain.repository

data class UserProfile(
    val id: String,
    val displayName: String,
    val healthGoal: String?,
    val weightKg: Double?,
    val heightCm: Double?,
    val activityLevel: String?,
    val dailyCalorieGoal: Int?,
    val createdAt: String,
)

data class ProfileUpdate(
    val displayName: String,
    val healthGoal: String?,
    val weightKg: Double?,
    val heightCm: Double?,
    val activityLevel: String?,
    val dailyCalorieGoal: Int?,
)

sealed interface ProfileResult<out T> {
    data class Success<T>(val value: T) : ProfileResult<T>
    data class Error(val message: String, val retryable: Boolean = false) : ProfileResult<Nothing>
}

interface ProfileRepository {
    suspend fun get(): ProfileResult<UserProfile>
    suspend fun update(update: ProfileUpdate): ProfileResult<UserProfile>
}
