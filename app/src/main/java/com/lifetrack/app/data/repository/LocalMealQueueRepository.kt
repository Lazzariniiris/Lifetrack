package com.lifetrack.app.data.repository

import com.lifetrack.app.data.local.MealDao
import com.lifetrack.app.data.local.MealHistoryEntity
import com.lifetrack.app.data.local.PendingMealAnalysisEntity
import com.lifetrack.app.data.remote.MealAnalysisResult
import com.lifetrack.app.domain.repository.MealQueueRepository
import com.lifetrack.app.domain.repository.MealHistoryItem
import com.lifetrack.app.domain.repository.QueuedMeal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton class LocalMealQueueRepository @Inject constructor(private val dao: MealDao, private val json: Json) : MealQueueRepository {
    override fun observeReady(): Flow<List<QueuedMeal>> = dao.observeReady().map { rows -> rows.mapNotNull { row -> row.resultJson?.let { QueuedMeal(row.id, row.photoPath, json.decodeFromString(it)) } } }
    override fun observeHistory(): Flow<List<MealAnalysisResult>> = dao.observeHistory().map { rows -> rows.map { json.decodeFromString(it.resultJson) } }
    override fun observeHistoryWithDates(): Flow<List<MealHistoryItem>> = dao.observeHistory().map { rows ->
        rows.map { MealHistoryItem(json.decodeFromString(it.resultJson), it.createdAt) }
    }
    override suspend fun enqueue(photoPath: String): String {
        val id = UUID.randomUUID().toString(); val now = System.currentTimeMillis()
        dao.insertPending(PendingMealAnalysisEntity(id, photoPath, "PENDING", null, now, now)); return id
    }
    override suspend fun nextPending(): QueuedMeal? = dao.nextPending()?.let { QueuedMeal(it.id, it.photoPath) }
    override suspend fun markReady(id: String, result: MealAnalysisResult) = dao.markReady(id, json.encodeToString(result), System.currentTimeMillis())
    override suspend fun markFailed(id: String) = dao.markFailed(id, System.currentTimeMillis())
    override suspend fun remove(id: String) = dao.deletePending(id)
    override suspend fun saveHistory(result: MealAnalysisResult) {
        val nutrition = result.nutrition
        dao.insertHistory(MealHistoryEntity(result.id ?: UUID.randomUUID().toString(), json.encodeToString(result), nutrition.calories, nutrition.proteinG, nutrition.carbsG, nutrition.fatG, nutrition.fiberG, nutrition.sugarsG, nutrition.sodiumMg, System.currentTimeMillis()))
    }
}
