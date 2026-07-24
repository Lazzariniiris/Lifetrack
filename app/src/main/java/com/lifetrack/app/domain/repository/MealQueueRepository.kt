package com.lifetrack.app.domain.repository

import com.lifetrack.app.data.remote.MealAnalysisResult
import kotlinx.coroutines.flow.Flow

data class QueuedMeal(val id: String, val photoPath: String, val result: MealAnalysisResult? = null)
data class MealHistoryItem(val result: MealAnalysisResult, val createdAt: Long)
interface MealQueueRepository {
    fun observeReady(): Flow<List<QueuedMeal>>
    fun observeHistory(): Flow<List<MealAnalysisResult>>
    fun observeHistoryWithDates(): Flow<List<MealHistoryItem>>
    suspend fun enqueue(photoPath: String): String
    suspend fun nextPending(): QueuedMeal?
    suspend fun markReady(id: String, result: MealAnalysisResult)
    suspend fun markFailed(id: String)
    suspend fun remove(id: String)
    suspend fun saveHistory(result: MealAnalysisResult)
}
