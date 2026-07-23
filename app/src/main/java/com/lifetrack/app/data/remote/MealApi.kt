package com.lifetrack.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

@Serializable data class DetectedFood(val name: String, val ingredients: List<String>, @SerialName("estimated_portion") val estimatedPortion: String)
@Serializable data class NutritionResult(
    val calories: Double,
    @SerialName("protein_g") val proteinG: Double,
    @SerialName("carbs_g") val carbsG: Double,
    @SerialName("fat_g") val fatG: Double,
    @SerialName("fiber_g") val fiberG: Double,
    @SerialName("sugars_g") val sugarsG: Double,
    @SerialName("sodium_mg") val sodiumMg: Double,
)
@Serializable data class MealAnalysisResult(val id: String? = null, val foods: List<DetectedFood>, val nutrition: NutritionResult, val confidence: String, val disclaimer: String)
interface MealApi {
    @Multipart @POST("v1/meals/analyze") suspend fun analyze(
        @Header("Authorization") authorization: String,
        @Part image: MultipartBody.Part,
        @Part("consent") consent: RequestBody,
    ): MealAnalysisResult
    @POST("v1/meals") suspend fun save(@Header("Authorization") authorization: String, @retrofit2.http.Body result: MealAnalysisResult): MealAnalysisResult
}
