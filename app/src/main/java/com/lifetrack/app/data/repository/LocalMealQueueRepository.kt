package com.lifetrack.app.data.repository

import com.lifetrack.app.data.local.MealDao
import com.lifetrack.app.data.local.MealHistoryEntity
import com.lifetrack.app.data.local.PendingMealAnalysisEntity
import com.lifetrack.app.data.remote.MealAnalysisResult
import com.lifetrack.app.domain.repository.MealHistoryItem
import com.lifetrack.app.domain.repository.MealQueueRepository
import com.lifetrack.app.domain.repository.MealQueueStatus
import com.lifetrack.app.domain.repository.QueuedMeal
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class LocalMealQueueRepository @Inject constructor(
    private val dao: MealDao,
    private val json: Json,
) : MealQueueRepository {
    override fun observeReady(ownerUserId: String): Flow<List<QueuedMeal>> = dao.observeReady(ownerUserId).map { rows -> rows.map(::toDomain) }

    override fun observeQueue(ownerUserId: String): Flow<List<QueuedMeal>> = dao.observeQueue(ownerUserId).map { rows -> rows.map(::toDomain) }

    override fun observeHistory(ownerUserId: String): Flow<List<MealAnalysisResult>> = dao.observeHistory(ownerUserId).map { rows ->
        rows.mapNotNull { runCatching { json.decodeFromString<MealAnalysisResult>(it.resultJson) }.getOrNull() }
    }

    override fun observeHistoryWithDates(ownerUserId: String): Flow<List<MealHistoryItem>> = dao.observeHistory(ownerUserId).map { rows ->
        rows.mapNotNull { row ->
            runCatching { MealHistoryItem(json.decodeFromString<MealAnalysisResult>(row.resultJson), row.createdAt) }.getOrNull()
        }
    }

    override suspend fun enqueue(ownerUserId: String, photoPath: String, id: String?): String {
        val queueId = id ?: UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        dao.insertPending(
            PendingMealAnalysisEntity(
                id = queueId,
                ownerUserId = ownerUserId,
                photoPath = photoPath,
                cloudPhotoPath = null,
                status = MealQueueStatus.PENDING.name,
                resultJson = null,
                lastError = null,
                attemptCount = 0,
                createdAt = now,
                updatedAt = now,
            ),
        )
        return queueId
    }

    override suspend fun nextPending(ownerUserId: String): QueuedMeal? = dao.nextPending(ownerUserId)?.let(::toDomain)
    override suspend fun markAttempt(id: String, cloudPhotoPath: String?) = dao.markAttempt(id, cloudPhotoPath, System.currentTimeMillis())
    override suspend fun markReady(id: String, result: MealAnalysisResult) = dao.markReady(id, json.encodeToString(result), System.currentTimeMillis())
    override suspend fun markFailed(id: String, message: String) = dao.markFailed(id, message, System.currentTimeMillis())
    override suspend fun retry(id: String, ownerUserId: String) = dao.retry(id, ownerUserId, System.currentTimeMillis())
    override suspend fun remove(id: String) = dao.deletePending(id)

    override suspend fun saveHistory(ownerUserId: String, result: MealAnalysisResult) {
        val nutrition = result.nutrition
        val now = System.currentTimeMillis()
        val createdAt = result.createdAt?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() } ?: now
        val updatedAt = result.updatedAt?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() } ?: now
        dao.insertHistory(
            MealHistoryEntity(
                id = result.id ?: UUID.randomUUID().toString(),
                ownerUserId = ownerUserId,
                resultJson = json.encodeToString(result),
                calories = nutrition.calories,
                proteinG = nutrition.proteinG,
                carbsG = nutrition.carbsG,
                fatG = nutrition.fatG,
                fiberG = nutrition.fiberG,
                sugarsG = nutrition.sugarsG,
                sodiumMg = nutrition.sodiumMg,
                createdAt = createdAt,
                updatedAt = updatedAt,
            ),
        )
    }

    override suspend fun deleteHistory(id: String, ownerUserId: String) = dao.deleteHistory(id, ownerUserId)
    override suspend fun allPendingPhotoPaths(): Set<String> = dao.allPendingPhotoPaths().toSet()

    private fun toDomain(row: PendingMealAnalysisEntity): QueuedMeal = QueuedMeal(
        id = row.id,
        ownerUserId = row.ownerUserId,
        photoPath = row.photoPath,
        cloudPhotoPath = row.cloudPhotoPath,
        status = runCatching { MealQueueStatus.valueOf(row.status) }.getOrDefault(MealQueueStatus.FAILED),
        result = row.resultJson?.let { runCatching { json.decodeFromString<MealAnalysisResult>(it) }.getOrNull() },
        lastError = row.lastError,
        attemptCount = row.attemptCount,
    )
}
