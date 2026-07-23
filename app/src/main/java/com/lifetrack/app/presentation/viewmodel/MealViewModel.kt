package com.lifetrack.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifetrack.app.data.remote.MealAnalysisResult
import com.lifetrack.app.domain.model.AppResult
import com.lifetrack.app.domain.repository.MealRepository
import com.lifetrack.app.domain.repository.MealQueueRepository
import com.lifetrack.app.sync.MealRetryScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import java.io.File

data class MealUiState(val photoPath: String? = null, val consent: Boolean = false, val loading: Boolean = false, val result: MealAnalysisResult? = null, val error: String? = null, val queuedId: String? = null, val history: List<MealAnalysisResult> = emptyList())
@HiltViewModel class MealViewModel @Inject constructor(private val repository: MealRepository, private val queue: MealQueueRepository, private val scheduler: MealRetryScheduler) : ViewModel() {
    private val mutableState = MutableStateFlow(MealUiState()); val state: StateFlow<MealUiState> = mutableState.asStateFlow()
    init {
        viewModelScope.launch { queue.observeReady().collect { ready -> ready.firstOrNull()?.let { item -> if (mutableState.value.result == null) mutableState.update { it.copy(result = item.result, queuedId = item.id, error = null) } } } }
        viewModelScope.launch { queue.observeHistory().collect { history -> mutableState.update { it.copy(history = history) } } }
        scheduler.enqueue()
    }
    fun setPhoto(path: String) { mutableState.value = mutableState.value.copy(photoPath = path, result = null, error = null) }
    fun setConsent(value: Boolean) { mutableState.value = mutableState.value.copy(consent = value) }
    fun submitPhoto(path: String) { setPhoto(path); analyze() }
    fun analyze() = viewModelScope.launch {
        val path = mutableState.value.photoPath ?: return@launch
        if (!mutableState.value.consent) { mutableState.value = mutableState.value.copy(error = "Confirma el consentimiento antes de enviar la foto."); return@launch }
        mutableState.value = mutableState.value.copy(loading = true, error = null)
        when (val response = repository.analyze(path)) {
            is AppResult.Error -> {
                val queuedId = queue.enqueue(path); scheduler.enqueue()
                mutableState.value = mutableState.value.copy(loading = false, photoPath = null, queuedId = queuedId, error = "Guardamos la foto de forma segura y la analizaremos automaticamente cuando vuelva la conexion.")
            }
            is AppResult.Success -> mutableState.value = mutableState.value.copy(loading = false, result = response.value, photoPath = null)
        }
    }
    fun saveEdited(result: MealAnalysisResult) = viewModelScope.launch {
        with(result.nutrition) {
            if (!validNutrition(calories, proteinG, carbsG, fatG, fiberG, sugarsG, sodiumMg)) {
                mutableState.value = mutableState.value.copy(error = "Revisa los valores nutricionales antes de guardar.")
                return@launch
            }
        }
        mutableState.value = mutableState.value.copy(loading = true, error = null)
        when (val response = repository.save(result)) {
            is AppResult.Error -> mutableState.value = mutableState.value.copy(loading = false, error = response.message)
            is AppResult.Success -> {
                queue.saveHistory(response.value)
                mutableState.value.queuedId?.let { queue.remove(it) }
                mutableState.value = mutableState.value.copy(loading = false, result = response.value, queuedId = null)
            }
        }
    }
    fun setCaptureError(message: String) { mutableState.value = mutableState.value.copy(error = message) }
    override fun onCleared() { mutableState.value.photoPath?.let { File(it).delete() }; super.onCleared() }
}

internal fun validNutrition(vararg values: Double): Boolean = values.all { it.isFinite() && it >= 0.0 }
