package com.lifetrack.app.domain.usecase

import com.lifetrack.app.domain.model.AppResult
import com.lifetrack.app.domain.model.Habit
import com.lifetrack.app.domain.model.HabitLog
import com.lifetrack.app.domain.model.HabitTargetType
import com.lifetrack.app.domain.model.SleepEntry
import com.lifetrack.app.domain.model.WaterEntry
import com.lifetrack.app.domain.repository.HabitRepository
import com.lifetrack.app.domain.repository.SleepRepository
import com.lifetrack.app.domain.repository.WaterRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

class HabitUseCases @Inject constructor(
    private val repository: HabitRepository,
) {
    fun observeActive(): Flow<List<Habit>> = repository.observeActive()
    fun observeLogs(): Flow<List<HabitLog>> = repository.observeLogs()

    suspend fun createHabit(
        name: String,
        description: String,
        type: HabitTargetType,
        targetValue: Int,
        now: Long = System.currentTimeMillis(),
    ): AppResult<Unit> {
        val normalizedName = name.trim()
        if (normalizedName.isEmpty()) return AppResult.Error("El nombre del habito es obligatorio.")
        if (normalizedName.length > MAX_HABIT_NAME_LENGTH) return AppResult.Error("El nombre admite hasta 80 caracteres.")
        if (targetValue !in MIN_TARGET_VALUE..MAX_TARGET_VALUE) {
            return AppResult.Error("El objetivo debe estar entre 1 y 10.000.")
        }
        repository.saveHabit(
            Habit(
                id = UUID.randomUUID().toString(),
                name = normalizedName,
                description = description.trim().takeIf { it.isNotEmpty() },
                targetType = type,
                targetValue = targetValue,
                color = DEFAULT_HABIT_COLOR,
                isActive = true,
                createdAt = now,
            ),
        )
        return AppResult.Success(Unit)
    }

    suspend fun logProgress(habit: Habit, value: Int, now: Long = System.currentTimeMillis()): AppResult<Unit> {
        if (value !in MIN_TARGET_VALUE..MAX_TARGET_VALUE) {
            return AppResult.Error("El valor debe estar entre 1 y 10.000.")
        }
        repository.addLog(HabitLog(UUID.randomUUID().toString(), habit.id, now, value))
        return AppResult.Success(Unit)
    }

    suspend fun archiveHabit(id: String) = repository.archiveHabit(id)

    private companion object {
        const val MAX_HABIT_NAME_LENGTH = 80
        const val MIN_TARGET_VALUE = 1
        const val MAX_TARGET_VALUE = 10_000
        const val DEFAULT_HABIT_COLOR = 0xFF2F6B4F
    }
}

class WaterUseCases @Inject constructor(
    private val repository: WaterRepository,
) {
    fun observeEntries(): Flow<List<WaterEntry>> = repository.observeEntries()

    suspend fun addEntry(amountMl: Int, now: Long = System.currentTimeMillis()): AppResult<Unit> {
        if (amountMl !in MIN_WATER_ENTRY_ML..MAX_WATER_ENTRY_ML) {
            return AppResult.Error("La cantidad debe estar entre 1 y 2.000 ml.")
        }
        repository.addEntry(WaterEntry(UUID.randomUUID().toString(), amountMl, now))
        return AppResult.Success(Unit)
    }

    suspend fun deleteEntry(id: String) = repository.deleteEntry(id)

    private companion object {
        const val MIN_WATER_ENTRY_ML = 1
        const val MAX_WATER_ENTRY_ML = 2_000
    }
}

class SleepUseCases @Inject constructor(
    private val repository: SleepRepository,
) {
    fun observeEntries(): Flow<List<SleepEntry>> = repository.observeEntries()

    suspend fun addEntry(
        bedtime: Long,
        wakeTime: Long,
        quality: Int,
        notes: String,
    ): AppResult<Unit> {
        val durationMinutes = (wakeTime - bedtime) / 60_000
        if (wakeTime <= bedtime || durationMinutes > MAX_SLEEP_DURATION_MINUTES) {
            return AppResult.Error("La hora de levantarse debe ser posterior y la sesion no puede superar 24 horas.")
        }
        if (quality !in 1..5) return AppResult.Error("La calidad debe estar entre 1 y 5.")
        repository.saveEntry(
            SleepEntry(
                id = UUID.randomUUID().toString(),
                bedtime = bedtime,
                wakeTime = wakeTime,
                quality = quality,
                notes = notes.trim().takeIf { it.isNotEmpty() },
            ),
        )
        return AppResult.Success(Unit)
    }

    suspend fun deleteEntry(id: String) = repository.deleteEntry(id)

    private companion object {
        const val MAX_SLEEP_DURATION_MINUTES = 24 * 60L
    }
}

data class DailyTrackingSummary(
    val completedHabits: Int,
    val totalHabits: Int,
    val waterMl: Int,
    val waterGoalMl: Int,
    val hasSleep: Boolean,
) {
    val habitProgress: Float get() = if (totalHabits == 0) 0f else completedHabits.toFloat() / totalHabits
    val waterProgress: Float get() = (waterMl.toFloat() / waterGoalMl).coerceIn(0f, 1f)
    val overallProgress: Float
        get() {
            val values = buildList {
                if (totalHabits > 0) add(habitProgress)
                add(waterProgress)
                if (hasSleep) add(1f)
            }
            return values.average().toFloat()
        }
}

fun summarizeDay(
    habits: List<Habit>,
    logs: List<HabitLog>,
    waterEntries: List<WaterEntry>,
    sleepEntries: List<SleepEntry>,
    waterGoalMl: Int,
    date: LocalDate = LocalDate.now(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): DailyTrackingSummary {
    fun Long.isOnDate() = Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate() == date
    val todayLogs = logs.filter { it.loggedAt.isOnDate() }
    val completed = habits.count { habit ->
        todayLogs.filter { it.habitId == habit.id }.sumOf { it.value } >= habit.targetValue
    }
    return DailyTrackingSummary(
        completedHabits = completed,
        totalHabits = habits.size,
        waterMl = waterEntries.filter { it.loggedAt.isOnDate() }.sumOf { it.amountMl },
        waterGoalMl = waterGoalMl,
        hasSleep = sleepEntries.any { it.wakeTime.isOnDate() },
    )
}
