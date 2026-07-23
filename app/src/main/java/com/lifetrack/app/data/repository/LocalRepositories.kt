package com.lifetrack.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lifetrack.app.data.local.HabitDao
import com.lifetrack.app.data.local.HabitEntity
import com.lifetrack.app.data.local.HabitLogDao
import com.lifetrack.app.data.local.HabitLogEntity
import com.lifetrack.app.data.local.SleepDao
import com.lifetrack.app.data.local.SleepEntryEntity
import com.lifetrack.app.data.local.WaterDao
import com.lifetrack.app.data.local.WaterEntryEntity
import com.lifetrack.app.domain.model.Habit
import com.lifetrack.app.domain.model.HabitLog
import com.lifetrack.app.domain.model.HabitTargetType
import com.lifetrack.app.domain.model.SleepEntry
import com.lifetrack.app.domain.model.ThemeMode
import com.lifetrack.app.domain.model.UserPreferences
import com.lifetrack.app.domain.model.WaterEntry
import com.lifetrack.app.domain.repository.HabitRepository
import com.lifetrack.app.domain.repository.PreferencesRepository
import com.lifetrack.app.domain.repository.SleepRepository
import com.lifetrack.app.domain.repository.WaterRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "lifetrack_preferences")

@Singleton
class LocalHabitRepository @Inject constructor(
    private val habitDao: HabitDao,
    private val habitLogDao: HabitLogDao,
) : HabitRepository {
    override fun observeActive(): Flow<List<Habit>> = habitDao.observeActive().map { entities ->
        entities.map { it.toDomain() }
    }

    override fun observeLogs(): Flow<List<HabitLog>> = habitLogDao.observeAll().map { entities ->
        entities.map { HabitLog(it.id, it.habitId, it.loggedAt, it.value) }
    }

    override suspend fun saveHabit(habit: Habit) = habitDao.insert(habit.toEntity())

    override suspend fun archiveHabit(id: String) = habitDao.archive(id)

    override suspend fun addLog(log: HabitLog) = habitLogDao.insert(
        HabitLogEntity(log.id, log.habitId, log.loggedAt, log.value),
    )
}

@Singleton
class LocalWaterRepository @Inject constructor(
    private val waterDao: WaterDao,
) : WaterRepository {
    override fun observeEntries(): Flow<List<WaterEntry>> = waterDao.observeAll().map { entities ->
        entities.map { WaterEntry(it.id, it.amountMl, it.loggedAt) }
    }

    override suspend fun addEntry(entry: WaterEntry) = waterDao.insert(
        WaterEntryEntity(entry.id, entry.amountMl, entry.loggedAt),
    )

    override suspend fun deleteEntry(id: String) = waterDao.delete(id)
}

@Singleton
class LocalSleepRepository @Inject constructor(
    private val sleepDao: SleepDao,
) : SleepRepository {
    override fun observeEntries(): Flow<List<SleepEntry>> = sleepDao.observeAll().map { entities ->
        entities.map { SleepEntry(it.id, it.bedtime, it.wakeTime, it.quality, it.notes) }
    }

    override suspend fun saveEntry(entry: SleepEntry) = sleepDao.insert(
        SleepEntryEntity(entry.id, entry.bedtime, entry.wakeTime, entry.quality, entry.notes),
    )

    override suspend fun deleteEntry(id: String) = sleepDao.delete(id)
}

@Singleton
class DataStorePreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : PreferencesRepository {
    override val preferences: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        UserPreferences(
            themeMode = preferences[THEME_MODE]?.let { value ->
                runCatching { ThemeMode.valueOf(value) }.getOrDefault(ThemeMode.SYSTEM)
            } ?: ThemeMode.SYSTEM,
            waterGoalMl = preferences[WATER_GOAL_ML] ?: 2_000,
            waterQuickAddMl = preferences[WATER_QUICK_ADD_ML] ?: 250,
            waterRemindersEnabled = preferences[WATER_REMINDERS_ENABLED] ?: false,
            quietStartMinutes = preferences[QUIET_START_MINUTES] ?: 1_320,
            quietEndMinutes = preferences[QUIET_END_MINUTES] ?: 420,
        )
    }

    override suspend fun updateWaterSettings(goalMl: Int, quickAddMl: Int, remindersEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[WATER_GOAL_ML] = goalMl
            preferences[WATER_QUICK_ADD_ML] = quickAddMl
            preferences[WATER_REMINDERS_ENABLED] = remindersEnabled
        }
    }

    override suspend fun updateThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences -> preferences[THEME_MODE] = mode.name }
    }

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val WATER_GOAL_ML = intPreferencesKey("water_goal_ml")
        val WATER_QUICK_ADD_ML = intPreferencesKey("water_quick_add_ml")
        val WATER_REMINDERS_ENABLED = booleanPreferencesKey("water_reminders_enabled")
        val QUIET_START_MINUTES = intPreferencesKey("quiet_start_minutes")
        val QUIET_END_MINUTES = intPreferencesKey("quiet_end_minutes")
    }
}

private fun HabitEntity.toDomain() = Habit(
    id = id,
    name = name,
    description = description,
    targetType = HabitTargetType.valueOf(targetType),
    targetValue = targetValue,
    color = color,
    isActive = isActive,
    createdAt = createdAt,
)

private fun Habit.toEntity() = HabitEntity(
    id = id,
    name = name,
    description = description,
    targetType = targetType.name,
    targetValue = targetValue,
    color = color,
    isActive = isActive,
    createdAt = createdAt,
)
