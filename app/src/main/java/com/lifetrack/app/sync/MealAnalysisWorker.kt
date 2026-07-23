package com.lifetrack.app.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lifetrack.app.domain.model.AppResult
import com.lifetrack.app.domain.repository.MealQueueRepository
import com.lifetrack.app.domain.repository.MealRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton class MealRetryScheduler @Inject constructor(@ApplicationContext private val context: Context) {
    fun enqueue() {
        val request = OneTimeWorkRequestBuilder<MealAnalysisWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }
}

@HiltWorker class MealAnalysisWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val queue: MealQueueRepository,
    private val remote: MealRepository,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        repeat(MAX_ITEMS_PER_RUN) {
            val pending = queue.nextPending() ?: return Result.success()
            when (val analysis = remote.analyze(pending.photoPath)) {
                is AppResult.Error -> return Result.retry()
                is AppResult.Success -> {
                    queue.markReady(pending.id, analysis.value)
                    File(pending.photoPath).delete()
                }
            }
        }
        return Result.success()
    }
    private companion object { const val MAX_ITEMS_PER_RUN = 3 }
}

private const val WORK_NAME = "pending_meal_analysis"
