package com.lifetrack.app.domain.usecase

import com.lifetrack.app.domain.model.AppResult
import com.lifetrack.app.domain.model.Habit
import com.lifetrack.app.domain.model.HabitLog
import com.lifetrack.app.domain.model.HabitTargetType
import com.lifetrack.app.domain.model.SleepEntry
import com.lifetrack.app.domain.model.WaterEntry
import com.lifetrack.app.domain.model.UserPreferences
import com.lifetrack.app.notifications.adaptiveWaterReminderDelay
import com.lifetrack.app.domain.repository.HabitRepository
import com.lifetrack.app.domain.repository.SleepRepository
import com.lifetrack.app.domain.repository.WaterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class TrackingUseCasesTest {
    @Test
    fun `habit rejects a blank name`() = runTest {
        val repository = FakeHabitRepository()
        val result = HabitUseCases(repository).createHabit("   ", "", HabitTargetType.YES_NO, 1)

        assertTrue(result is AppResult.Error)
        assertTrue(repository.habits.value.isEmpty())
    }

    @Test
    fun `habit is normalized before persistence`() = runTest {
        val repository = FakeHabitRepository()
        val result = HabitUseCases(repository).createHabit("  Caminar  ", "  Diario  ", HabitTargetType.YES_NO, 1, now = 100L)

        assertTrue(result is AppResult.Success)
        assertEquals("Caminar", repository.habits.value.single().name)
        assertEquals("Diario", repository.habits.value.single().description)
    }

    @Test
    fun `water entry rejects an unsafe amount`() = runTest {
        val repository = FakeWaterRepository()
        val result = WaterUseCases(repository).addEntry(2_001)

        assertTrue(result is AppResult.Error)
        assertTrue(repository.entries.value.isEmpty())
    }

    @Test
    fun `sleep accepts a session spanning midnight`() = runTest {
        val repository = FakeSleepRepository()
        val bedtime = LocalDate.of(2026, 7, 1).atTime(23, 0).atZone(ZoneId.of("UTC")).toInstant().toEpochMilli()
        val wakeTime = LocalDate.of(2026, 7, 2).atTime(7, 30).atZone(ZoneId.of("UTC")).toInstant().toEpochMilli()

        val result = SleepUseCases(repository).addEntry(bedtime, wakeTime, 4, "Descanso normal")

        assertTrue(result is AppResult.Success)
        assertEquals(510L, repository.entries.value.single().durationMinutes)
    }

    @Test
    fun `daily summary weights active tracking components`() {
        val zone = ZoneId.of("UTC")
        val day = LocalDate.of(2026, 7, 10)
        val timestamp = day.atTime(10, 0).atZone(zone).toInstant().toEpochMilli()
        val habit = Habit("habit", "Leer", null, HabitTargetType.YES_NO, 1, 0L, true, timestamp)

        val summary = summarizeDay(
            habits = listOf(habit),
            logs = listOf(HabitLog("log", "habit", timestamp, 1)),
            waterEntries = listOf(WaterEntry("water", 1_000, timestamp)),
            sleepEntries = emptyList(),
            waterGoalMl = 2_000,
            date = day,
            zoneId = zone,
        )

        assertEquals(1, summary.completedHabits)
        assertEquals(0.75f, summary.overallProgress, 0.001f)
    }

    @Test
    fun `adaptive reminder uses minimum interval when hydration is behind pace`() {
        val preferences = UserPreferences(waterGoalMl = 2_000, waterQuickAddMl = 250)
        val delay = adaptiveWaterReminderDelay(
            entries = emptyList(),
            preferences = preferences,
            now = LocalDateTime.of(2026, 7, 10, 10, 0),
            zoneId = ZoneId.of("UTC"),
        )

        assertEquals(45L, delay)
    }

    @Test
    fun `adaptive reminder schedules the next active day after water goal is reached`() {
        val timestamp = LocalDateTime.of(2026, 7, 10, 9, 0).atZone(ZoneId.of("UTC")).toInstant().toEpochMilli()
        val delay = adaptiveWaterReminderDelay(
            entries = listOf(WaterEntry("water", 2_000, timestamp)),
            preferences = UserPreferences(waterGoalMl = 2_000),
            now = LocalDateTime.of(2026, 7, 10, 10, 0),
            zoneId = ZoneId.of("UTC"),
        )

        assertEquals(1_260L, delay)
    }

    @Test
    fun `adaptive reminder waits for next active window instead of notifying before quiet hours`() {
        val delay = adaptiveWaterReminderDelay(
            entries = emptyList(),
            preferences = UserPreferences(),
            now = LocalDateTime.of(2026, 7, 10, 21, 50),
            zoneId = ZoneId.of("UTC"),
        )

        assertEquals(550L, delay)
    }
}

private class FakeHabitRepository : HabitRepository {
    val habits = MutableStateFlow<List<Habit>>(emptyList())
    val logs = MutableStateFlow<List<HabitLog>>(emptyList())
    override fun observeActive(): Flow<List<Habit>> = habits
    override fun observeLogs(): Flow<List<HabitLog>> = logs
    override suspend fun saveHabit(habit: Habit) { habits.value = habits.value + habit }
    override suspend fun archiveHabit(id: String) { habits.value = habits.value.filterNot { it.id == id } }
    override suspend fun addLog(log: HabitLog) { logs.value = logs.value + log }
}

private class FakeWaterRepository : WaterRepository {
    val entries = MutableStateFlow<List<WaterEntry>>(emptyList())
    override fun observeEntries(): Flow<List<WaterEntry>> = entries
    override suspend fun addEntry(entry: WaterEntry) { entries.value = entries.value + entry }
    override suspend fun deleteEntry(id: String) { entries.value = entries.value.filterNot { it.id == id } }
}

private class FakeSleepRepository : SleepRepository {
    val entries = MutableStateFlow<List<SleepEntry>>(emptyList())
    override fun observeEntries(): Flow<List<SleepEntry>> = entries
    override suspend fun saveEntry(entry: SleepEntry) { entries.value = entries.value + entry }
    override suspend fun deleteEntry(id: String) { entries.value = entries.value.filterNot { it.id == id } }
}
