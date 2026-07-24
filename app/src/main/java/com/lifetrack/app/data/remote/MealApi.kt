package com.lifetrack.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class DetectedFood(
    val name: String,
    val ingredients: List<String> = emptyList(),
    @SerialName("estimated_portion") val estimatedPortion: String,
    @SerialName("estimated_grams") val estimatedGrams: Double = 0.0,
    val confidence: Double = 0.0,
    val alternatives: List<String> = emptyList(),
)

@Serializable
data class NutritionResult(
    val calories: Double,
    @SerialName("protein_g") val proteinG: Double,
    @SerialName("carbs_g") val carbsG: Double,
    @SerialName("fat_g") val fatG: Double,
    @SerialName("fiber_g") val fiberG: Double,
    @SerialName("sugars_g") val sugarsG: Double,
    @SerialName("sodium_mg") val sodiumMg: Double,
)

@Serializable
data class MealAnalysisResult(
    val id: String? = null,
    val foods: List<DetectedFood>,
    val nutrition: NutritionResult,
    val confidence: Double,
    val observations: List<String> = emptyList(),
    val disclaimer: String = "",
    val status: String = "completed",
    @SerialName("photo_path") val photoPath: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class MealCreateRequest(
    val id: String,
    val foods: List<DetectedFood>,
    val nutrition: NutritionResult,
    val confidence: Double,
    val observations: List<String>,
    @SerialName("photo_path") val photoPath: String? = null,
)

@Serializable
data class MealPage(
    val items: List<MealAnalysisResult>,
    val limit: Int,
    val offset: Int,
    val total: Int,
)

@Serializable
data class PendingMealCloudRow(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("foods_json") val foods: List<DetectedFood> = emptyList(),
    val calories: Double? = null,
    @SerialName("protein_g") val proteinG: Double? = null,
    @SerialName("carbs_g") val carbsG: Double? = null,
    @SerialName("fat_g") val fatG: Double? = null,
    @SerialName("fiber_g") val fiberG: Double? = null,
    @SerialName("sugars_g") val sugarsG: Double? = null,
    @SerialName("sodium_mg") val sodiumMg: Double? = null,
    val status: String = "pending",
    @SerialName("photo_path") val photoPath: String,
    val confidence: Double = 0.0,
    val observations: List<String> = emptyList(),
)

interface MealApi {
    @Multipart
    @POST("v1/meals/analyze")
    suspend fun analyze(
        @Header("Authorization") authorization: String,
        @Part image: MultipartBody.Part,
        @Part("consent") consent: RequestBody,
    ): MealAnalysisResult

    @POST("v1/meals")
    suspend fun save(
        @Header("Authorization") authorization: String,
        @Body result: MealCreateRequest,
    ): MealAnalysisResult

    @GET("v1/meals")
    suspend fun list(
        @Header("Authorization") authorization: String,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
    ): MealPage

    @DELETE("v1/meals/{id}")
    suspend fun delete(@Header("Authorization") authorization: String, @Path("id") id: String): Response<Unit>
}

interface MealCloudApi {
    @PUT("storage/v1/object/meal-images/{path}")
    @Headers("Content-Type: image/jpeg", "x-upsert: true")
    suspend fun uploadImage(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Path(value = "path", encoded = true) path: String,
        @Body image: RequestBody,
    ): Response<Unit>

    @POST("rest/v1/meal_analyses")
    @Headers("Prefer: resolution=ignore-duplicates,return=minimal")
    suspend fun createPending(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("on_conflict") onConflict: String = "id",
        @Body row: PendingMealCloudRow,
    ): Response<Unit>

    @DELETE("storage/v1/object/meal-images/{path}")
    suspend fun deleteImage(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Path(value = "path", encoded = true) path: String,
    ): Response<Unit>
}
