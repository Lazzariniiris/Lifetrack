package com.lifetrack.app.data.repository

import com.lifetrack.app.BuildConfig
import com.lifetrack.app.data.remote.ProfileApi
import com.lifetrack.app.data.remote.ProfileUpdateDto
import com.lifetrack.app.data.remote.UserProfileDto
import com.lifetrack.app.domain.repository.AuthRepository
import com.lifetrack.app.domain.repository.ProfileRepository
import com.lifetrack.app.domain.repository.ProfileResult
import com.lifetrack.app.domain.repository.ProfileUpdate
import com.lifetrack.app.domain.repository.UserProfile
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import retrofit2.HttpException

@Singleton
class SupabaseProfileRepository @Inject constructor(
    private val api: ProfileApi,
    private val auth: AuthRepository,
) : ProfileRepository {
    override suspend fun get(): ProfileResult<UserProfile> = execute { userId, token ->
        api.getProfile(BuildConfig.SUPABASE_ANON_KEY, "Bearer $token", id = "eq.$userId").singleOrNull()?.toDomain()
            ?: error("missing_profile")
    }

    override suspend fun update(update: ProfileUpdate): ProfileResult<UserProfile> = execute { userId, token ->
        require(update.displayName.trim().length in 1..80)
        require(update.weightKg == null || update.weightKg in 20.0..500.0)
        require(update.heightCm == null || update.heightCm in 80.0..250.0)
        require(update.dailyCalorieGoal == null || update.dailyCalorieGoal in 500..10_000)
        api.updateProfile(
            BuildConfig.SUPABASE_ANON_KEY,
            "Bearer $token",
            id = "eq.$userId",
            update = ProfileUpdateDto(
                displayName = update.displayName.trim(),
                healthGoal = update.healthGoal?.trim()?.takeIf(String::isNotEmpty),
                weightKg = update.weightKg,
                heightCm = update.heightCm,
                activityLevel = update.activityLevel,
                dailyCalorieGoal = update.dailyCalorieGoal,
            ),
        ).singleOrNull()?.toDomain() ?: error("missing_profile")
    }

    private suspend fun <T> execute(block: suspend (String, String) -> T): ProfileResult<T> {
        val user = auth.user.first() ?: return ProfileResult.Error("Iniciá sesión para acceder al perfil.")
        val token = auth.validAccessToken() ?: return ProfileResult.Error("Tu sesión venció. Iniciá sesión nuevamente.")
        return try {
            ProfileResult.Success(block(user.id, token))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: IOException) {
            ProfileResult.Error("No pudimos conectarnos para actualizar el perfil.", retryable = true)
        } catch (error: HttpException) {
            if (error.code() in 500..599 || error.code() == 429) ProfileResult.Error("El perfil no está disponible ahora.", retryable = true)
            else ProfileResult.Error("No pudimos guardar el perfil. Revisá los datos.")
        } catch (_: IllegalArgumentException) {
            ProfileResult.Error("Revisá los valores ingresados en el perfil.")
        } catch (_: Throwable) {
            ProfileResult.Error("No pudimos completar la operación con el perfil.")
        }
    }
}

private fun UserProfileDto.toDomain() = UserProfile(
    id = id,
    displayName = displayName.orEmpty(),
    healthGoal = healthGoal,
    weightKg = weightKg,
    heightCm = heightCm,
    activityLevel = activityLevel,
    dailyCalorieGoal = dailyCalorieGoal,
    createdAt = createdAt,
)
