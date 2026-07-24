package com.lifetrack.app.data.repository

import com.lifetrack.app.BuildConfig
import com.lifetrack.app.data.remote.MealAnalysisResult
import com.lifetrack.app.data.remote.MealApi
import com.lifetrack.app.data.remote.MealCloudApi
import com.lifetrack.app.data.remote.MealCreateRequest
import com.lifetrack.app.data.remote.PendingMealCloudRow
import com.lifetrack.app.domain.model.AppResult
import com.lifetrack.app.domain.repository.AuthRepository
import com.lifetrack.app.domain.repository.MealRepository
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException

@Singleton
class RemoteMealRepository @Inject constructor(
    private val api: MealApi,
    private val cloudApi: MealCloudApi,
    private val auth: AuthRepository,
) : MealRepository {
    private val cloudConfigured = BuildConfig.SUPABASE_URL.startsWith("https://") && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()
    override val configured = BuildConfig.API_BASE_URL.startsWith("https://") && cloudConfigured

    override suspend fun preparePending(id: String, ownerUserId: String, photoPath: String): AppResult<String> {
        if (!cloudConfigured) return AppResult.Error("El almacenamiento seguro no está configurado en esta instalación.")
        val token = auth.validAccessToken() ?: return AppResult.Error("Iniciá sesión para guardar la fotografía.")
        val file = File(photoPath)
        if (!file.exists()) return AppResult.Error("La fotografía ya no está disponible.")
        val cloudPath = "$ownerUserId/$id.jpg"
        return executeRemote {
            val authorization = "Bearer $token"
            val upload = cloudApi.uploadImage(
                BuildConfig.SUPABASE_ANON_KEY,
                authorization,
                cloudPath,
                file.asRequestBody("image/jpeg".toMediaType()),
            )
            if (!upload.isSuccessful) throw HttpStatusException(upload.code())
            val pending = cloudApi.createPending(
                BuildConfig.SUPABASE_ANON_KEY,
                authorization,
                row = PendingMealCloudRow(id = id, userId = ownerUserId, photoPath = cloudPath),
            )
            if (!pending.isSuccessful && pending.code() != 409) throw HttpStatusException(pending.code())
            cloudPath
        }
    }

    override suspend fun analyze(photoPath: String): AppResult<MealAnalysisResult> {
        if (!configured) return AppResult.Error("El análisis está temporalmente fuera de servicio.", retryable = true)
        val token = auth.validAccessToken() ?: return AppResult.Error("Iniciá sesión para analizar esta comida.")
        val file = File(photoPath)
        if (!file.exists()) return AppResult.Error("La fotografía ya no está disponible. Podés tomar otra.")
        return executeRemote {
            val image = MultipartBody.Part.createFormData("image", "meal.jpg", file.asRequestBody("image/jpeg".toMediaType()))
            api.analyze("Bearer $token", image, "true".toRequestBody("text/plain".toMediaType()))
        }
    }

    override suspend fun save(result: MealAnalysisResult): AppResult<MealAnalysisResult> {
        if (!configured) return AppResult.Error("El servicio de comidas no está disponible.", retryable = true)
        val token = auth.validAccessToken() ?: return AppResult.Error("Iniciá sesión para guardar la comida.")
        val id = result.id ?: return AppResult.Error("No pudimos identificar este análisis. Volvé a intentarlo.")
        return executeRemote {
            api.save(
                "Bearer $token",
                MealCreateRequest(id, result.foods, result.nutrition, result.confidence, result.observations, result.photoPath),
            )
        }
    }

    override suspend fun list(): AppResult<List<MealAnalysisResult>> {
        if (!configured) return AppResult.Error("El historial remoto no está disponible.", retryable = true)
        val token = auth.validAccessToken() ?: return AppResult.Error("Iniciá sesión para recuperar tus comidas.")
        return executeRemote { api.list("Bearer $token").items }
    }

    override suspend fun delete(id: String): AppResult<Unit> {
        if (!configured) return AppResult.Error("No pudimos eliminar la comida ahora.", retryable = true)
        val token = auth.validAccessToken() ?: return AppResult.Error("Iniciá sesión para eliminar la comida.")
        return executeRemote {
            val response = api.delete("Bearer $token", id)
            if (!response.isSuccessful && response.code() != 404) throw HttpStatusException(response.code())
        }
    }

    private suspend fun <T> executeRemote(block: suspend () -> T): AppResult<T> = try {
        AppResult.Success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: HttpException) {
        mealHttpError(error.code())
    } catch (error: HttpStatusException) {
        mealHttpError(error.status)
    } catch (_: SocketTimeoutException) {
        AppResult.Error("El servicio tardó demasiado. Reintentaremos automáticamente.", retryable = true)
    } catch (_: IOException) {
        AppResult.Error("No hay conexión con el servicio. Reintentaremos automáticamente.", retryable = true)
    } catch (_: Throwable) {
        AppResult.Error("No pudimos completar el análisis en este momento.", retryable = true)
    }
}

private class HttpStatusException(val status: Int) : RuntimeException()

internal fun mealHttpError(status: Int): AppResult.Error = when (status) {
    400, 415, 422 -> AppResult.Error("La fotografía no pudo procesarse. Elegí otra imagen con buena iluminación.")
    401, 403 -> AppResult.Error("Tu sesión venció. Iniciá sesión nuevamente.")
    413 -> AppResult.Error("La fotografía es demasiado grande. Tomá otra o elegí una imagen más liviana.")
    429 -> AppResult.Error("El servicio está ocupado. Reintentaremos en unos minutos.", retryable = true)
    in 500..599 -> AppResult.Error("El análisis está temporalmente fuera de servicio.", retryable = true)
    else -> AppResult.Error("No pudimos completar la operación.")
}
