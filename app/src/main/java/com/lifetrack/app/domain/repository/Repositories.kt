package com.lifetrack.app.domain.repository

import com.lifetrack.app.domain.model.Habit
import com.lifetrack.app.domain.model.HabitLog
import com.lifetrack.app.domain.model.SleepEntry
import com.lifetrack.app.domain.model.UserPreferences
import com.lifetrack.app.domain.model.WaterEntry
import kotlinx.coroutines.flow.Flow

interface HabitRepository {
    fun observeActive(): Flow<List<Habit>>
    fun observeLogs(): Flow<List<HabitLog>>
    suspend fun saveHabit(habit: Habit)
    suspend fun archiveHabit(id: String)
    suspend fun addLog(log: HabitLog)
}

interface WaterRepository {
    fun observeEntries(): Flow<List<WaterEntry>>
    suspend fun addEntry(entry: WaterEntry)
    suspend fun deleteEntry(id: String)
}

interface SleepRepository {
    fun observeEntries(): Flow<List<SleepEntry>>
    suspend fun saveEntry(entry: SleepEntry)
    suspend fun deleteEntry(id: String)
}

interface PreferencesRepository {
    val preferences: Flow<UserPreferences>
    suspend fun updateWaterSettings(
        goalMl: Int,
        quickAddMl: Int,
        remindersEnabled: Boolean,
    )
    suspend fun updateThemeMode(mode: com.lifetrack.app.domain.model.ThemeMode)
}
