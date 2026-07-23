package com.lifetrack.app.domain.repository

import com.lifetrack.app.data.remote.MealAnalysisResult
import com.lifetrack.app.domain.model.AppResult

interface MealRepository {
    val configured: Boolean
    suspend fun analyze(photoPath: String): AppResult<MealAnalysisResult>
    suspend fun save(result: MealAnalysisResult): AppResult<MealAnalysisResult>
}
