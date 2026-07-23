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
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lifetrack.app.domain.repository.PreferencesRepository
import com.lifetrack.app.domain.repository.WaterRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

interface ReminderScheduler {
    fun updateWaterReminders(enabled: Boolean)
}

@Singleton
class WorkManagerReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : ReminderScheduler {
    override fun updateWaterReminders(enabled: Boolean) {
        val workManager = WorkManager.getInstance(context)
        if (!enabled) {
            workManager.cancelUniqueWork(WATER_REMINDER_WORK)
            return
        }
        val request = PeriodicWorkRequestBuilder<WaterReminderWorker>(1, TimeUnit.HOURS)
            .setInitialDelay(1, TimeUnit.HOURS)
            .addTag(WATER_REMINDER_WORK)
            .build()
        workManager.enqueueUniquePeriodicWork(
            WATER_REMINDER_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
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
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val preferences = preferencesRepository.preferences.first()
        if (!preferences.waterRemindersEnabled || isQuietTime(preferences.quietStartMinutes, preferences.quietEndMinutes)) {
            return Result.success()
        }
        val today = java.time.LocalDate.now()
        val consumed = waterRepository.observeEntries().first().filter { entry ->
            java.time.Instant.ofEpochMilli(entry.loggedAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate() == today
        }.sumOf { it.amountMl }
        if (consumed >= preferences.waterGoalMl) return Result.success()
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }
        createChannel(applicationContext)
        val remaining = preferences.waterGoalMl - consumed
        val notification = NotificationCompat.Builder(applicationContext, WATER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Recordatorio de hidratacion")
            .setContentText("Faltan $remaining ml para tu objetivo diario.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        val manager = ContextCompat.getSystemService(applicationContext, NotificationManager::class.java)
        manager?.notify(WATER_NOTIFICATION_ID, notification)
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
