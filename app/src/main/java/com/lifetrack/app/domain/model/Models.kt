package com.lifetrack.app.domain.model

enum class HabitTargetType {
    YES_NO,
    QUANTITY,
    DURATION,
    REPETITIONS,
}

data class Habit(
    val id: String,
    val name: String,
    val description: String?,
    val targetType: HabitTargetType,
    val targetValue: Int,
    val color: Long,
    val isActive: Boolean,
    val createdAt: Long,
)

data class HabitLog(
    val id: String,
    val habitId: String,
    val loggedAt: Long,
    val value: Int,
)

data class WaterEntry(
    val id: String,
    val amountMl: Int,
    val loggedAt: Long,
)

data class SleepEntry(
    val id: String,
    val bedtime: Long,
    val wakeTime: Long,
    val quality: Int,
    val notes: String?,
) {
    val durationMinutes: Long get() = (wakeTime - bedtime) / 60_000
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val waterGoalMl: Int = DEFAULT_WATER_GOAL_ML,
    val waterQuickAddMl: Int = DEFAULT_WATER_QUICK_ADD_ML,
    val waterRemindersEnabled: Boolean = false,
    val quietStartMinutes: Int = DEFAULT_QUIET_START_MINUTES,
    val quietEndMinutes: Int = DEFAULT_QUIET_END_MINUTES,
)

const val DEFAULT_WATER_GOAL_ML = 2_000
const val DEFAULT_WATER_QUICK_ADD_ML = 250
const val DEFAULT_QUIET_START_MINUTES = 22 * 60
const val DEFAULT_QUIET_END_MINUTES = 7 * 60

sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Error(val message: String, val retryable: Boolean = false) : AppResult<Nothing>
}
