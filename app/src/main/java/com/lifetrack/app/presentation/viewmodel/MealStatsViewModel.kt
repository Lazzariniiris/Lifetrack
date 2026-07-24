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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class MealStatsUiState(
    val count: Int = 0,
    val averageCalories: Int = 0,
    val averageProteinG: Int = 0,
    val weekCount: Int = 0,
)
@HiltViewModel class MealStatsViewModel @Inject constructor(queue: MealQueueRepository) : ViewModel() {
    val state: StateFlow<MealStatsUiState> = queue.observeHistoryWithDates().map { history ->
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val todayMeals = history.filter { Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate() == today }.map { it.result }
        val weekStart = today.minusDays(6)
        val weekCount = history.count { !Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate().isBefore(weekStart) }
        MealStatsUiState(
            count = todayMeals.size,
            averageCalories = todayMeals.map { it.nutrition.calories }.average().takeUnless(Double::isNaN)?.toInt() ?: 0,
            averageProteinG = todayMeals.map { it.nutrition.proteinG }.average().takeUnless(Double::isNaN)?.toInt() ?: 0,
            weekCount = weekCount,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MealStatsUiState())
}
