package com.lifetrack.app.domain.repository

import com.lifetrack.app.data.remote.MealAnalysisResult
import com.lifetrack.app.domain.model.AppResult

interface MealRepository {
    val configured: Boolean
    suspend fun preparePending(id: String, ownerUserId: String, photoPath: String): AppResult<String>
    suspend fun analyze(photoPath: String): AppResult<MealAnalysisResult>
    suspend fun save(result: MealAnalysisResult): AppResult<MealAnalysisResult>
    suspend fun list(): AppResult<List<MealAnalysisResult>>
    suspend fun delete(id: String): AppResult<Unit>
}
