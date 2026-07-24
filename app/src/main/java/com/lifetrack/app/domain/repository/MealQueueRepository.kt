package com.lifetrack.app.domain.repository

import com.lifetrack.app.data.remote.MealAnalysisResult
import kotlinx.coroutines.flow.Flow

enum class MealQueueStatus { PENDING, READY, FAILED }

data class QueuedMeal(
    val id: String,
    val ownerUserId: String,
    val photoPath: String,
    val cloudPhotoPath: String? = null,
    val status: MealQueueStatus = MealQueueStatus.PENDING,
    val result: MealAnalysisResult? = null,
    val lastError: String? = null,
    val attemptCount: Int = 0,
)

data class MealHistoryItem(val result: MealAnalysisResult, val createdAt: Long)

interface MealQueueRepository {
    fun observeReady(ownerUserId: String): Flow<List<QueuedMeal>>
    fun observeQueue(ownerUserId: String): Flow<List<QueuedMeal>>
    fun observeHistory(ownerUserId: String): Flow<List<MealAnalysisResult>>
    fun observeHistoryWithDates(ownerUserId: String): Flow<List<MealHistoryItem>>
    suspend fun enqueue(ownerUserId: String, photoPath: String, id: String? = null): String
    suspend fun nextPending(ownerUserId: String): QueuedMeal?
    suspend fun markAttempt(id: String, cloudPhotoPath: String?)
    suspend fun markReady(id: String, result: MealAnalysisResult)
    suspend fun markFailed(id: String, message: String)
    suspend fun retry(id: String, ownerUserId: String)
    suspend fun remove(id: String)
    suspend fun saveHistory(ownerUserId: String, result: MealAnalysisResult)
    suspend fun deleteHistory(id: String, ownerUserId: String)
    suspend fun allPendingPhotoPaths(): Set<String>
}
