package com.lifetrack.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifetrack.app.data.remote.MealAnalysisResult
import com.lifetrack.app.domain.model.AppResult
import com.lifetrack.app.domain.repository.AuthRepository
import com.lifetrack.app.domain.repository.MealQueueRepository
import com.lifetrack.app.domain.repository.MealRepository
import com.lifetrack.app.domain.repository.QueuedMeal
import com.lifetrack.app.sync.MealRetryScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

data class MealUiState(
    val photoPath: String? = null,
    val consent: Boolean = false,
    val loading: Boolean = false,
    val result: MealAnalysisResult? = null,
    val error: String? = null,
    val notice: String? = null,
    val queuedId: String? = null,
    val queue: List<QueuedMeal> = emptyList(),
    val history: List<MealAnalysisResult> = emptyList(),
    val serviceConfigured: Boolean = true,
    val ownerUserId: String? = null,
    val authInitialized: Boolean = false,
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class MealViewModel @Inject constructor(
    private val repository: MealRepository,
    private val queueRepository: MealQueueRepository,
    private val scheduler: MealRetryScheduler,
    private val auth: AuthRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val mutableState = MutableStateFlow(MealUiState(serviceConfigured = repository.configured))
    val state: StateFlow<MealUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch { cleanOrphanedPhotos() }
        viewModelScope.launch {
            auth.user.collectLatest { user ->
                val previousOwner = mutableState.value.ownerUserId
                if (previousOwner != user?.id) {
                    val current = mutableState.value
                    if (current.queuedId == null) current.photoPath?.let { File(it).delete() }
                    mutableState.value = MealUiState(
                        serviceConfigured = repository.configured,
                        ownerUserId = user?.id,
                        authInitialized = true,
                    )
                } else {
                    mutableState.update { it.copy(authInitialized = true) }
                }
                if (user != null) {
                    scheduler.enqueue()
                    syncRemoteHistory(user.id)
                }
            }
        }
        viewModelScope.launch {
            auth.user.flatMapLatest { user ->
                if (user == null) flowOf(emptyList()) else queueRepository.observeQueue(user.id)
            }.collect { items -> mutableState.update { it.copy(queue = items) } }
        }
        viewModelScope.launch {
            auth.user.flatMapLatest { user ->
                if (user == null) flowOf(emptyList()) else queueRepository.observeHistory(user.id)
            }.collect { history -> mutableState.update { it.copy(history = history) } }
        }
        viewModelScope.launch {
            auth.user.flatMapLatest { user ->
                if (user == null) flowOf(emptyList()) else queueRepository.observeReady(user.id)
            }.collect { ready ->
                ready.firstOrNull()?.let { item ->
                    if (mutableState.value.result == null) {
                        mutableState.update { it.copy(result = item.result, queuedId = item.id, error = null, notice = null) }
                    }
                }
            }
        }
    }

    fun setPhoto(path: String) {
        mutableState.value.photoPath?.takeIf { it != path && mutableState.value.queuedId == null }?.let { File(it).delete() }
        mutableState.update { it.copy(photoPath = path, result = null, queuedId = null, error = null, notice = null) }
    }

    fun setConsent(value: Boolean) = mutableState.update { it.copy(consent = value) }

    fun analyze() = viewModelScope.launch {
        val state = mutableState.value
        val path = state.photoPath ?: return@launch
        val owner = state.ownerUserId
        if (owner == null) {
            mutableState.update { it.copy(error = "Iniciá sesión desde Perfil antes de analizar la fotografía.") }
            return@launch
        }
        if (!state.consent) {
            mutableState.update { it.copy(error = "Confirmá el procesamiento temporal de la fotografía.") }
            return@launch
        }

        val queueId = state.queuedId ?: queueRepository.enqueue(owner, path)
        mutableState.update { it.copy(queuedId = queueId, loading = true, error = null, notice = null) }

        val cloudPath = when (val preparation = repository.preparePending(queueId, owner, path)) {
            is AppResult.Success -> preparation.value
            is AppResult.Error -> {
                queueRepository.markAttempt(queueId, null)
                if (preparation.retryable) {
                    scheduler.enqueue()
                    mutableState.update {
                        it.copy(
                            loading = false,
                            photoPath = null,
                            notice = "La fotografía está guardada de forma segura y pendiente de análisis. Reintentaremos automáticamente.",
                        )
                    }
                } else {
                    queueRepository.markFailed(queueId, preparation.message)
                    mutableState.update { it.copy(loading = false, error = preparation.message) }
                }
                return@launch
            }
        }
        queueRepository.markAttempt(queueId, cloudPath)

        when (val response = repository.analyze(path)) {
            is AppResult.Error -> {
                if (response.retryable) {
                    scheduler.enqueue()
                    mutableState.update {
                        it.copy(
                            loading = false,
                            photoPath = null,
                            notice = "La fotografía quedó pendiente de análisis. Te avisaremos cuando el resultado esté listo.",
                        )
                    }
                } else {
                    queueRepository.markFailed(queueId, response.message)
                    mutableState.update { it.copy(loading = false, error = response.message) }
                }
            }
            is AppResult.Success -> {
                val result = response.value.copy(id = queueId, photoPath = cloudPath, status = "review")
                queueRepository.markReady(queueId, result)
                mutableState.update { it.copy(loading = false, result = result, error = null) }
            }
        }
    }

    fun saveEdited(result: MealAnalysisResult) = viewModelScope.launch {
        val owner = mutableState.value.ownerUserId ?: return@launch
        with(result.nutrition) {
            if (!validNutrition(calories, proteinG, carbsG, fatG, fiberG, sugarsG, sodiumMg)) {
                mutableState.update { it.copy(error = "Revisá los valores nutricionales antes de guardar.") }
                return@launch
            }
        }
        mutableState.update { it.copy(loading = true, error = null, notice = null) }
        when (val response = repository.save(result)) {
            is AppResult.Error -> mutableState.update { it.copy(loading = false, error = response.message) }
            is AppResult.Success -> {
                queueRepository.saveHistory(owner, response.value)
                val queued = mutableState.value.queue.firstOrNull { it.id == response.value.id }
                mutableState.value.queuedId?.let { queueRepository.remove(it) }
                queued?.photoPath?.let { File(it).delete() }
                mutableState.update { it.copy(loading = false, result = response.value, queuedId = null) }
            }
        }
    }

    fun retryQueued(id: String) = viewModelScope.launch {
        val owner = mutableState.value.ownerUserId ?: return@launch
        queueRepository.retry(id, owner)
        scheduler.enqueue()
    }

    fun removeQueued(id: String) = viewModelScope.launch {
        val queued = mutableState.value.queue.firstOrNull { it.id == id }
        mutableState.update { it.copy(loading = true, error = null, notice = null) }
        when (val result = repository.delete(id)) {
            is AppResult.Error -> mutableState.update { it.copy(loading = false, error = "No pudimos eliminar la fotografía de forma segura. Reintentá cuando tengas conexión.") }
            is AppResult.Success -> {
                queueRepository.remove(id)
                queued?.photoPath?.let { File(it).delete() }
                mutableState.update { it.copy(loading = false, notice = "Fotografía pendiente eliminada.") }
            }
        }
    }

    fun deleteMeal(id: String) = viewModelScope.launch {
        val owner = mutableState.value.ownerUserId ?: return@launch
        mutableState.update { it.copy(loading = true, error = null) }
        when (val result = repository.delete(id)) {
            is AppResult.Error -> mutableState.update { it.copy(loading = false, error = result.message) }
            is AppResult.Success -> {
                queueRepository.deleteHistory(id, owner)
                mutableState.update { it.copy(loading = false, notice = "Comida eliminada.") }
            }
        }
    }

    fun setCaptureError(message: String) = mutableState.update { it.copy(error = message) }
    fun clearFeedback() = mutableState.update { it.copy(error = null, notice = null) }

    fun startOver() {
        val state = mutableState.value
        if (state.queuedId == null) state.photoPath?.let { File(it).delete() }
        mutableState.update { it.copy(photoPath = null, result = null, error = null, notice = null, queuedId = null) }
    }

    private suspend fun syncRemoteHistory(ownerUserId: String) {
        when (val remote = repository.list()) {
            is AppResult.Error -> if (!remote.retryable) mutableState.update { it.copy(error = remote.message) }
            is AppResult.Success -> remote.value.forEach { queueRepository.saveHistory(ownerUserId, it) }
        }
    }

    private suspend fun cleanOrphanedPhotos() {
        val referenced = queueRepository.allPendingPhotoPaths()
        val cutoff = System.currentTimeMillis() - ORPHAN_MAX_AGE_MS
        File(context.filesDir, "pending_meals").listFiles()?.forEach { file ->
            if (file.isFile && file.absolutePath !in referenced && file.lastModified() < cutoff) file.delete()
        }
    }

    override fun onCleared() {
        val state = mutableState.value
        if (state.queuedId == null) state.photoPath?.let { File(it).delete() }
        super.onCleared()
    }
}

private const val ORPHAN_MAX_AGE_MS = 24 * 60 * 60 * 1_000L

internal fun validNutrition(vararg values: Double): Boolean = values.all { it.isFinite() && it >= 0.0 }
