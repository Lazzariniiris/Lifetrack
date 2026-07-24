package com.lifetrack.app.sync

import android.content.Context
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lifetrack.app.domain.model.AppResult
import com.lifetrack.app.domain.repository.AuthRepository
import com.lifetrack.app.domain.repository.MealQueueRepository
import com.lifetrack.app.domain.repository.MealRepository
import com.lifetrack.app.domain.repository.PreferencesRepository
import com.lifetrack.app.MainActivity
import com.lifetrack.app.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class MealRetryScheduler @Inject constructor(@ApplicationContext private val context: Context) {
    fun enqueue() {
        val request = OneTimeWorkRequestBuilder<MealAnalysisWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }
}

@HiltWorker
class MealAnalysisWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val queue: MealQueueRepository,
    private val remote: MealRepository,
    private val auth: AuthRepository,
    private val preferences: PreferencesRepository,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val owner = auth.user.first() ?: return Result.success()
        repeat(MAX_ITEMS_PER_RUN) {
            val pending = queue.nextPending(owner.id) ?: return Result.success()
            if (pending.attemptCount >= MAX_ATTEMPTS) {
                queue.markFailed(pending.id, "No pudimos completar el análisis después de varios intentos. Podés reintentarlo manualmente.")
                return@repeat
            }

            var cloudPath = pending.cloudPhotoPath
            if (cloudPath == null) {
                when (val preparation = remote.preparePending(pending.id, owner.id, pending.photoPath)) {
                    is AppResult.Success -> cloudPath = preparation.value
                    is AppResult.Error -> {
                        queue.markAttempt(pending.id, null)
                        if (preparation.retryable) return Result.retry()
                        queue.markFailed(pending.id, preparation.message)
                        return@repeat
                    }
                }
            }
            queue.markAttempt(pending.id, cloudPath)

            when (val analysis = remote.analyze(pending.photoPath)) {
                is AppResult.Error -> {
                    if (analysis.retryable) return Result.retry()
                    queue.markFailed(pending.id, analysis.message)
                }
                is AppResult.Success -> {
                    queue.markReady(pending.id, analysis.value.copy(id = pending.id, photoPath = cloudPath, status = "review"))
                    if (preferences.preferences.first().mealAnalysisNotificationsEnabled) notifyResultReady(pending.id)
                }
            }
        }
        return if (queue.nextPending(owner.id) != null) Result.retry() else Result.success()
    }

    private companion object {
        const val MAX_ITEMS_PER_RUN = 3
        const val MAX_ATTEMPTS = 8
        const val CHANNEL_ID = "meal_analysis"
    }

    private fun notifyResultReady(id: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            applicationContext.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Análisis de comidas", NotificationManager.IMPORTANCE_DEFAULT))
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_meals", true)
        }
        val pendingIntent = PendingIntent.getActivity(applicationContext, id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = androidx.core.app.NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Tu análisis está listo")
            .setContentText("Abrí LifeTrack para revisar y confirmar los alimentos.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        manager.notify(id.hashCode(), notification)
    }

}

private const val WORK_NAME = "pending_meal_analysis"
