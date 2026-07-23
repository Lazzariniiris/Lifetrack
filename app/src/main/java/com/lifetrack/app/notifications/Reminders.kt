package com.lifetrack.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lifetrack.app.domain.repository.PreferencesRepository
import com.lifetrack.app.domain.repository.WaterRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import com.lifetrack.app.domain.model.UserPreferences
import com.lifetrack.app.domain.model.WaterEntry
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

interface ReminderScheduler {
    suspend fun updateWaterReminders(enabled: Boolean)
    suspend fun refreshWaterReminder()
}

@Singleton
class WorkManagerReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val waterRepository: WaterRepository,
    private val preferencesRepository: PreferencesRepository,
) : ReminderScheduler {
    override suspend fun updateWaterReminders(enabled: Boolean) {
        if (!enabled) WorkManager.getInstance(context).cancelUniqueWork(WATER_REMINDER_WORK)
        else refreshWaterReminder()
    }

    override suspend fun refreshWaterReminder() {
        val workManager = WorkManager.getInstance(context)
        val preferences = preferencesRepository.preferences.first()
        val delayMinutes = adaptiveWaterReminderDelay(waterRepository.observeEntries().first(), preferences)
        if (!preferences.waterRemindersEnabled || delayMinutes == null) {
            workManager.cancelUniqueWork(WATER_REMINDER_WORK)
            return
        }
        val request = OneTimeWorkRequestBuilder<WaterReminderWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .addTag(WATER_REMINDER_WORK)
            .build()
        workManager.enqueueUniqueWork(
            WATER_REMINDER_WORK,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}

@HiltWorker
class WaterReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val waterRepository: WaterRepository,
    private val preferencesRepository: PreferencesRepository,
    private val reminderScheduler: ReminderScheduler,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val preferences = preferencesRepository.preferences.first()
        if (!preferences.waterRemindersEnabled) {
            return Result.success()
        }
        if (isQuietTime(preferences.quietStartMinutes, preferences.quietEndMinutes)) {
            reminderScheduler.refreshWaterReminder()
            return Result.success()
        }
        val today = java.time.LocalDate.now()
        val consumed = waterRepository.observeEntries().first().filter { entry ->
            java.time.Instant.ofEpochMilli(entry.loggedAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate() == today
        }.sumOf { it.amountMl }
        if (consumed >= preferences.waterGoalMl) {
            reminderScheduler.refreshWaterReminder()
            return Result.success()
        }
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }
        createChannel(applicationContext)
        val remaining = preferences.waterGoalMl - consumed
        val notification = NotificationCompat.Builder(applicationContext, WATER_CHANNEL_ID)
            .setSmallIcon(com.lifetrack.app.R.drawable.ic_notification)
            .setColor(0xFF25CBB4.toInt())
            .setContentTitle("Recordatorio de hidratacion")
            .setContentText("Faltan $remaining ml para tu objetivo diario.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        val manager = ContextCompat.getSystemService(applicationContext, NotificationManager::class.java)
        manager?.notify(WATER_NOTIFICATION_ID, notification)
        reminderScheduler.refreshWaterReminder()
        return Result.success()
    }

    private fun isQuietTime(startMinutes: Int, endMinutes: Int): Boolean {
        val nowMinutes = LocalTime.now().hour * 60 + LocalTime.now().minute
        return if (startMinutes > endMinutes) nowMinutes >= startMinutes || nowMinutes < endMinutes
        else nowMinutes in startMinutes until endMinutes
    }
}

private fun createChannel(context: Context) {
    val channel = NotificationChannel(
        WATER_CHANNEL_ID,
        "Recordatorios de hidratacion",
        NotificationManager.IMPORTANCE_DEFAULT,
    ).apply { description = "Recordatorios locales para registrar hidratacion." }
    ContextCompat.getSystemService(context, NotificationManager::class.java)?.createNotificationChannel(channel)
}

private const val WATER_REMINDER_WORK = "water_reminder_work"
private const val WATER_CHANNEL_ID = "water_reminders"
private const val WATER_NOTIFICATION_ID = 2_001

internal fun adaptiveWaterReminderDelay(
    entries: List<WaterEntry>,
    preferences: UserPreferences,
    now: LocalDateTime = LocalDateTime.now(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): Long? {
    val activeStart = LocalTime.of(preferences.quietEndMinutes / 60, preferences.quietEndMinutes % 60)
    val activeEnd = LocalTime.of(preferences.quietStartMinutes / 60, preferences.quietStartMinutes % 60)
    if (activeEnd <= activeStart) return null
    val today = now.toLocalDate()
    val currentTime = now.toLocalTime()
    fun nextActiveStartDelay(): Long {
        val nextStart = if (currentTime < activeStart) LocalDateTime.of(today, activeStart)
        else LocalDateTime.of(today.plusDays(1), activeStart)
        return Duration.between(now, nextStart).toMinutes().coerceAtLeast(1)
    }
    if (currentTime >= activeEnd || currentTime < activeStart) return nextActiveStartDelay()

    fun WaterEntry.localDate() = Instant.ofEpochMilli(loggedAt).atZone(zoneId).toLocalDate()
    val todayEntries = entries.filter { it.localDate() == today }
    val consumed = todayEntries.sumOf { it.amountMl }
    val remaining = preferences.waterGoalMl - consumed
    if (remaining <= 0) return nextActiveStartDelay()

    val minutesRemaining = Duration.between(now, LocalDateTime.of(today, activeEnd)).toMinutes().coerceAtLeast(1)
    val portionsRemaining = ((remaining + preferences.waterQuickAddMl - 1) / preferences.waterQuickAddMl).coerceAtLeast(1)
    val baselineInterval = (minutesRemaining / portionsRemaining).coerceIn(MIN_REMINDER_INTERVAL_MINUTES, MAX_REMINDER_INTERVAL_MINUTES)
    val habitualInterval = entries.groupBy { it.localDate() }.values.flatMap { dailyEntries ->
        dailyEntries.sortedBy { it.loggedAt }.zipWithNext { first, second ->
            Duration.between(Instant.ofEpochMilli(first.loggedAt), Instant.ofEpochMilli(second.loggedAt)).toMinutes()
        }
    }.filter { it in MIN_REMINDER_INTERVAL_MINUTES..MAX_REMINDER_INTERVAL_MINUTES }.average()
        .takeIf { !it.isNaN() }
        ?.toLong()
    val adjustedInterval = if (habitualInterval == null) baselineInterval else ((baselineInterval + habitualInterval) / 2)
        .coerceIn(MIN_REMINDER_INTERVAL_MINUTES, MAX_REMINDER_INTERVAL_MINUTES)
    val activeMinutes = Duration.between(activeStart, activeEnd).toMinutes().coerceAtLeast(1)
    val elapsedRatio = Duration.between(activeStart, currentTime).toMinutes().toFloat() / activeMinutes
    val consumedRatio = consumed.toFloat() / preferences.waterGoalMl
    val paceInterval = if (consumedRatio < elapsedRatio * 0.9f) MIN_REMINDER_INTERVAL_MINUTES else adjustedInterval
    val lastEntry = todayEntries.maxByOrNull { it.loggedAt }
    val sinceLast = lastEntry?.let { Duration.between(Instant.ofEpochMilli(it.loggedAt), now.atZone(zoneId).toInstant()).toMinutes() }
    val delay = if (sinceLast == null) paceInterval else maxOf(paceInterval, MIN_REMINDER_INTERVAL_MINUTES - sinceLast)
        .coerceIn(MIN_REMINDER_INTERVAL_MINUTES, MAX_REMINDER_INTERVAL_MINUTES)
    // Do not notify immediately before a silent period; resume at the next active window instead.
    return if (delay > minutesRemaining) nextActiveStartDelay() else delay
}

private const val MIN_REMINDER_INTERVAL_MINUTES = 45L
private const val MAX_REMINDER_INTERVAL_MINUTES = 120L
