package com.lifetrack.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifetrack.app.domain.repository.MealQueueRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class MealStatsUiState(val count: Int = 0, val averageCalories: Int = 0, val averageProteinG: Int = 0)
@HiltViewModel class MealStatsViewModel @Inject constructor(queue: MealQueueRepository) : ViewModel() {
    val state: StateFlow<MealStatsUiState> = queue.observeHistory().map { meals ->
        MealStatsUiState(
            count = meals.size,
            averageCalories = meals.map { it.nutrition.calories }.average().takeUnless(Double::isNaN)?.toInt() ?: 0,
            averageProteinG = meals.map { it.nutrition.proteinG }.average().takeUnless(Double::isNaN)?.toInt() ?: 0,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MealStatsUiState())
}
