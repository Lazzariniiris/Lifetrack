package com.lifetrack.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifetrack.app.data.remote.MealAnalysisResult
import com.lifetrack.app.domain.model.AppResult
import com.lifetrack.app.domain.repository.MealRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.io.File

data class MealUiState(val photoPath: String? = null, val consent: Boolean = false, val loading: Boolean = false, val result: MealAnalysisResult? = null, val error: String? = null, val configured: Boolean = false)
@HiltViewModel class MealViewModel @Inject constructor(private val repository: MealRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(MealUiState(configured = repository.configured)); val state: StateFlow<MealUiState> = mutableState.asStateFlow()
    fun setPhoto(path: String) { mutableState.value = mutableState.value.copy(photoPath = path, result = null, error = null) }
    fun setConsent(value: Boolean) { mutableState.value = mutableState.value.copy(consent = value) }
    fun analyze() = viewModelScope.launch {
        val path = mutableState.value.photoPath ?: return@launch
        if (!mutableState.value.consent) { mutableState.value = mutableState.value.copy(error = "Confirma el consentimiento antes de enviar la foto."); return@launch }
        mutableState.value = mutableState.value.copy(loading = true, error = null)
        when (val response = repository.analyze(path)) {
            is AppResult.Error -> mutableState.value = mutableState.value.copy(loading = false, error = response.message)
            is AppResult.Success -> mutableState.value = mutableState.value.copy(loading = false, result = response.value, photoPath = null)
        }
    }
    fun updateNutrition(calories: Double, protein: Double, carbs: Double, fat: Double) {
        val result = mutableState.value.result ?: return
        if (!validNutrition(calories, protein, carbs, fat)) {
            mutableState.value = mutableState.value.copy(error = "Los valores nutricionales deben ser numeros positivos.")
            return
        }
        mutableState.value = mutableState.value.copy(result = result.copy(nutrition = result.nutrition.copy(calories = calories, proteinG = protein, carbsG = carbs, fatG = fat)), error = null)
    }
    fun save() = viewModelScope.launch {
        val result = mutableState.value.result ?: return@launch
        with(result.nutrition) {
            if (!validNutrition(calories, proteinG, carbsG, fatG)) {
                mutableState.value = mutableState.value.copy(error = "Revisa los valores nutricionales antes de guardar.")
                return@launch
            }
        }
        mutableState.value = mutableState.value.copy(loading = true, error = null)
        when (val response = repository.save(result)) {
            is AppResult.Error -> mutableState.value = mutableState.value.copy(loading = false, error = response.message)
            is AppResult.Success -> mutableState.value = mutableState.value.copy(loading = false, result = response.value)
        }
    }
    fun setCaptureError(message: String) { mutableState.value = mutableState.value.copy(error = message) }
    override fun onCleared() { mutableState.value.photoPath?.let { File(it).delete() }; super.onCleared() }
}

internal fun validNutrition(vararg values: Double): Boolean = values.all { it.isFinite() && it >= 0.0 }
