package com.lifetrack.app.data.repository

import com.lifetrack.app.BuildConfig
import com.lifetrack.app.data.remote.MealAnalysisResult
import com.lifetrack.app.data.remote.MealApi
import com.lifetrack.app.domain.model.AppResult
import com.lifetrack.app.domain.repository.AuthRepository
import com.lifetrack.app.domain.repository.MealRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton class RemoteMealRepository @Inject constructor(private val api: MealApi, private val auth: AuthRepository) : MealRepository {
    override val configured = BuildConfig.API_BASE_URL.startsWith("https://")
    override suspend fun analyze(photoPath: String): AppResult<MealAnalysisResult> {
        val token = auth.validAccessToken() ?: return AppResult.Error("Inicia sesion para analizar una comida.")
        val file = File(photoPath); if (!file.exists()) return AppResult.Error("La foto temporal ya no esta disponible.")
        return runCatching {
            val image = MultipartBody.Part.createFormData("image", "meal.jpg", file.asRequestBody("image/jpeg".toMediaType()))
            api.analyze("Bearer $token", image, "true".toRequestBody("text/plain".toMediaType()))
        }.fold({ file.delete(); AppResult.Success(it) }, { AppResult.Error(it.message ?: "No se pudo analizar la comida") })
    }
    override suspend fun save(result: MealAnalysisResult): AppResult<MealAnalysisResult> {
        val token = auth.validAccessToken() ?: return AppResult.Error("Inicia sesion para guardar la comida.")
        return runCatching { api.save("Bearer $token", result.copy(id = null)) }
            .fold({ AppResult.Success(it) }, { AppResult.Error(it.message ?: "No se pudo guardar la comida") })
    }
}
