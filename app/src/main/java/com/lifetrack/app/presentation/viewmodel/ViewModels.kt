package com.lifetrack.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifetrack.app.domain.model.AppResult
import com.lifetrack.app.domain.model.Habit
import com.lifetrack.app.domain.model.HabitTargetType
import com.lifetrack.app.domain.model.ThemeMode
import com.lifetrack.app.domain.model.UserPreferences
import com.lifetrack.app.domain.model.WaterEntry
import com.lifetrack.app.domain.repository.PreferencesRepository
import com.lifetrack.app.domain.usecase.HabitUseCases
import com.lifetrack.app.domain.usecase.SleepUseCases
import com.lifetrack.app.domain.usecase.WaterUseCases
import com.lifetrack.app.domain.usecase.dailyMotivation
import com.lifetrack.app.domain.usecase.hydrationStreak
import com.lifetrack.app.domain.usecase.summarizeDay
import com.lifetrack.app.notifications.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this)
    .atZone(ZoneId.systemDefault())
    .toLocalDate()

@HiltViewModel
class AppViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {
    val preferences: StateFlow<UserPreferences> = preferencesRepository.preferences.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        UserPreferences(),
    )

    init {
        viewModelScope.launch { reminderScheduler.refreshWaterReminder() }
    }

    fun setTheme(mode: ThemeMode) = viewModelScope.launch {
        preferencesRepository.updateThemeMode(mode)
    }
}

data class HomeUiState(
    val greeting: String = "Buen día",
    val progress: Float = 0f,
    val completedHabits: Int = 0,
    val totalHabits: Int = 0,
    val waterMl: Int = 0,
    val waterGoalMl: Int = 2_000,
    val sleepMinutes: Long = 0,
    val nextAction: String = "Creá un hábito o registrá tu primera actividad.",
    val dailyMotivation: String = "Un pequeño paso hoy también cuenta.",
    val hydrationStreak: Int = 0,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    habitUseCases: HabitUseCases,
    waterUseCases: WaterUseCases,
    sleepUseCases: SleepUseCases,
    preferencesRepository: PreferencesRepository,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = combine(
        habitUseCases.observeActive(),
        habitUseCases.observeLogs(),
        waterUseCases.observeEntries(),
        sleepUseCases.observeEntries(),
        preferencesRepository.preferences,
    ) { habits, logs, water, sleep, preferences ->
        val summary = summarizeDay(habits, logs, water, sleep, preferences.waterGoalMl)
        val hour = java.time.LocalTime.now().hour
        val greeting = when (hour) {
            in 5..11 -> "Buen día"
            in 12..18 -> "Buenas tardes"
            else -> "Buenas noches"
        }
        val nextAction = when {
            summary.waterMl < summary.waterGoalMl -> "Faltan ${summary.waterGoalMl - summary.waterMl} ml para tu objetivo."
            summary.completedHabits < summary.totalHabits -> "Tenés un hábito pendiente para hoy."
            !summary.hasSleep -> "Registrá tu última sesión de sueño."
            else -> "Tu seguimiento de hoy está al día."
        }
        HomeUiState(
            greeting = greeting,
            progress = summary.overallProgress,
            completedHabits = summary.completedHabits,
            totalHabits = summary.totalHabits,
            waterMl = summary.waterMl,
            waterGoalMl = summary.waterGoalMl,
            sleepMinutes = sleep.filter { it.wakeTime.toLocalDate() == LocalDate.now() }.sumOf { it.durationMinutes },
            nextAction = nextAction,
            dailyMotivation = dailyMotivation(),
            hydrationStreak = hydrationStreak(water, preferences.waterGoalMl),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}

data class HabitListItem(
    val habit: Habit,
    val currentValue: Int,
    val isComplete: Boolean,
)

data class HabitsUiState(
    val items: List<HabitListItem> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val useCases: HabitUseCases,
) : ViewModel() {
    private val error = MutableStateFlow<String?>(null)
    val uiState: StateFlow<HabitsUiState> = combine(
        useCases.observeActive(),
        useCases.observeLogs(),
        error,
    ) { habits, logs, message ->
        val today = LocalDate.now()
        HabitsUiState(
            items = habits.map { habit ->
                val current = logs.filter { it.habitId == habit.id && it.loggedAt.toLocalDate() == today }.sumOf { it.value }
                HabitListItem(habit, current, current >= habit.targetValue)
            },
            error = message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HabitsUiState())

    fun createHabit(name: String, description: String, type: HabitTargetType, target: Int) = viewModelScope.launch {
        when (val result = useCases.createHabit(name, description, type, target)) {
            is AppResult.Error -> error.value = result.message
            is AppResult.Success -> error.value = null
        }
    }

    fun complete(item: HabitListItem) = viewModelScope.launch {
        val remaining = (item.habit.targetValue - item.currentValue).coerceAtLeast(1)
        when (val result = useCases.logProgress(item.habit, remaining)) {
            is AppResult.Error -> error.value = result.message
            is AppResult.Success -> error.value = null
        }
    }

    fun archive(id: String) = viewModelScope.launch { useCases.archiveHabit(id) }
    fun clearError() { error.value = null }
}

data class WaterUiState(
    val entries: List<WaterEntry> = emptyList(),
    val consumedMl: Int = 0,
    val preferences: UserPreferences = UserPreferences(),
    val error: String? = null,
)

@HiltViewModel
class WaterViewModel @Inject constructor(
    private val useCases: WaterUseCases,
    private val preferencesRepository: PreferencesRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {
    private val error = MutableStateFlow<String?>(null)
    val uiState: StateFlow<WaterUiState> = combine(
        useCases.observeEntries(),
        preferencesRepository.preferences,
        error,
    ) { entries, preferences, message ->
        WaterUiState(
            entries = entries,
            consumedMl = entries.filter { it.loggedAt.toLocalDate() == LocalDate.now() }.sumOf { it.amountMl },
            preferences = preferences,
            error = message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WaterUiState())

    fun add(amountMl: Int) = viewModelScope.launch {
        when (val result = useCases.addEntry(amountMl)) {
            is AppResult.Error -> error.value = result.message
            is AppResult.Success -> {
                reminderScheduler.refreshWaterReminder()
                error.value = null
            }
        }
    }

    fun delete(id: String) = viewModelScope.launch {
        useCases.deleteEntry(id)
        reminderScheduler.refreshWaterReminder()
    }

    fun updateSettings(goalMl: Int, quickAddMl: Int, remindersEnabled: Boolean) = viewModelScope.launch {
        if (goalMl !in 250..10_000 || quickAddMl !in 50..1_000) {
            error.value = "Objetivo entre 250 y 10.000 ml; registro rapido entre 50 y 1.000 ml."
        } else {
            preferencesRepository.updateWaterSettings(goalMl, quickAddMl, remindersEnabled)
            reminderScheduler.updateWaterReminders(remindersEnabled)
            error.value = null
        }
    }

    fun clearError() { error.value = null }
}

data class SleepUiState(
    val entries: List<com.lifetrack.app.domain.model.SleepEntry> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class SleepViewModel @Inject constructor(
    private val useCases: SleepUseCases,
) : ViewModel() {
    private val error = MutableStateFlow<String?>(null)
    val uiState: StateFlow<SleepUiState> = combine(useCases.observeEntries(), error) { entries, message ->
        SleepUiState(entries, message)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SleepUiState())

    fun add(bedtime: Long?, wakeTime: Long?, quality: Int, notes: String) = viewModelScope.launch {
        if (bedtime == null || wakeTime == null) {
            error.value = "Usa el formato dd/MM/aaaa HH:mm para las horas."
            return@launch
        }
        when (val result = useCases.addEntry(bedtime, wakeTime, quality, notes)) {
            is AppResult.Error -> error.value = result.message
            is AppResult.Success -> error.value = null
        }
    }

    fun delete(id: String) = viewModelScope.launch { useCases.deleteEntry(id) }
    fun clearError() { error.value = null }
}

data class StatisticsUiState(
    val weeklyWaterAverage: Int = 0,
    val weeklySleepAverage: Long = 0,
    val habitCompletion: Float = 0f,
    val daysWithData: Int = 0,
    val dailyWater: List<Int> = List(7) { 0 },
    val dailySleep: List<Long> = List(7) { 0L },
    val dailyHabitCompletion: List<Float> = List(7) { 0f },
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    habitUseCases: HabitUseCases,
    waterUseCases: WaterUseCases,
    sleepUseCases: SleepUseCases,
    preferencesRepository: PreferencesRepository,
) : ViewModel() {
    val uiState: StateFlow<StatisticsUiState> = combine(
        habitUseCases.observeActive(),
        habitUseCases.observeLogs(),
        waterUseCases.observeEntries(),
        sleepUseCases.observeEntries(),
        preferencesRepository.preferences,
    ) { habits, logs, water, sleep, preferences ->
        val days = (0..6).map { LocalDate.now().minusDays(it.toLong()) }
        val waterByDay = days.map { day -> water.filter { it.loggedAt.toLocalDate() == day }.sumOf { it.amountMl } }
        val sleepByDay = days.map { day -> sleep.filter { it.wakeTime.toLocalDate() == day }.map { it.durationMinutes } }
        val possible = habits.size * days.size
        val completed = days.sumOf { day ->
            habits.count { habit -> logs.filter { it.habitId == habit.id && it.loggedAt.toLocalDate() == day }.sumOf { it.value } >= habit.targetValue }
        }
        val recordedDays = days.count { day ->
            water.any { it.loggedAt.toLocalDate() == day } || sleep.any { it.wakeTime.toLocalDate() == day } || logs.any { it.loggedAt.toLocalDate() == day }
        }
        val habitByDay = days.map { day ->
            if (habits.isEmpty()) 0f else habits.count { habit ->
                logs.filter { it.habitId == habit.id && it.loggedAt.toLocalDate() == day }.sumOf { it.value } >= habit.targetValue
            }.toFloat() / habits.size
        }
        StatisticsUiState(
            weeklyWaterAverage = waterByDay.average().toInt(),
            weeklySleepAverage = sleepByDay.flatten().average().takeIf { !it.isNaN() }?.toLong() ?: 0,
            habitCompletion = if (possible == 0) 0f else completed.toFloat() / possible,
            daysWithData = recordedDays,
            dailyWater = waterByDay,
            dailySleep = sleepByDay.map { values -> values.average().takeIf { !it.isNaN() }?.toLong() ?: 0L },
            dailyHabitCompletion = habitByDay,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatisticsUiState())
}

data class CalendarUiState(
    val activityByDate: Map<LocalDate, Int> = emptyMap(),
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    habitUseCases: HabitUseCases,
    waterUseCases: WaterUseCases,
    sleepUseCases: SleepUseCases,
) : ViewModel() {
    val uiState: StateFlow<CalendarUiState> = combine(
        habitUseCases.observeLogs(),
        waterUseCases.observeEntries(),
        sleepUseCases.observeEntries(),
    ) { logs, water, sleep ->
        val activity = mutableMapOf<LocalDate, Int>()
        logs.forEach { activity[it.loggedAt.toLocalDate()] = (activity[it.loggedAt.toLocalDate()] ?: 0) + 1 }
        water.forEach { activity[it.loggedAt.toLocalDate()] = (activity[it.loggedAt.toLocalDate()] ?: 0) + 1 }
        sleep.forEach { activity[it.wakeTime.toLocalDate()] = (activity[it.wakeTime.toLocalDate()] ?: 0) + 1 }
        CalendarUiState(activity)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())
}
