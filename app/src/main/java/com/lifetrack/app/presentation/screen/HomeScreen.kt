package com.lifetrack.app.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifetrack.app.presentation.theme.LifeTrackColors
import com.lifetrack.app.presentation.viewmodel.HomeViewModel
import com.lifetrack.app.presentation.viewmodel.MealStatsViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    onNavigate: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    mealStatsViewModel: MealStatsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val meals by mealStatsViewModel.state.collectAsStateWithLifecycle()
    val habitProgress = if (state.totalHabits == 0) null else state.completedHabits.toFloat() / state.totalHabits
    val waterProgress = state.waterMl.toFloat() / state.waterGoalMl.coerceAtLeast(1)
    val sleepProgress = state.sleepMinutes.toFloat() / (8 * 60)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 24.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale("es", "AR"))).replaceFirstChar(Char::titlecase),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text("${state.greeting},", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                Text("este es tu resumen de hoy", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ProgressRing(
                        progress = state.progress,
                        label = "${(state.progress * 100).toInt()}%",
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Progreso diario", style = MaterialTheme.typography.titleLarge)
                        Text(state.nextAction, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                            Text(
                                if (state.hydrationStreak > 0) "${state.hydrationStreak} días de racha" else "Empezá tu racha hoy",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
        }

        item { SectionHeader("Tu día") }

        item {
            MetricRow {
                HealthMetricCard(
                    "Agua", "${state.waterMl} ml", "Objetivo ${state.waterGoalMl} ml",
                    Icons.Rounded.WaterDrop, LifeTrackColors.Water, waterProgress, Modifier.weight(1f),
                ) { onNavigate("water") }
                HealthMetricCard(
                    "Sueño", if (state.sleepMinutes > 0) formatMinutes(state.sleepMinutes) else "Sin registrar", "Objetivo 8 h",
                    Icons.Rounded.Bedtime, LifeTrackColors.Sleep, if (state.sleepMinutes > 0) sleepProgress else null, Modifier.weight(1f),
                ) { onNavigate("sleep") }
            }
        }
        item {
            MetricRow {
                HealthMetricCard(
                    "Hábitos", "${state.completedHabits}/${state.totalHabits}", "Completados hoy",
                    Icons.Rounded.CheckCircle, LifeTrackColors.Habits, habitProgress, Modifier.weight(1f),
                ) { onNavigate("habits") }
                HealthMetricCard(
                    "Alimentación", if (meals.count > 0) "${meals.averageCalories} kcal" else "Sin registrar", if (meals.count > 0) "Promedio por comida" else "Fotografiá tu comida",
                    Icons.Rounded.Restaurant, LifeTrackColors.Meals, null, Modifier.weight(1f),
                ) { onNavigate("meals") }
            }
        }
        item {
            MetricRow {
                HealthMetricCard(
                    "Actividad", "Sin registrar", "Movimiento diario",
                    Icons.Rounded.DirectionsRun, LifeTrackColors.Activity, null, Modifier.weight(1f),
                ) { onNavigate("statistics") }
                HealthMetricCard(
                    "Ánimo", "Sin registrar", "¿Cómo te sentís?",
                    Icons.Rounded.Mood, LifeTrackColors.Mood, null, Modifier.weight(1f),
                ) { onNavigate("statistics") }
            }
        }
        item {
            HealthMetricCard(
                "Progreso", "${(state.progress * 100).toInt()}%", "Objetivo diario combinado",
                Icons.Rounded.TrendingUp, MaterialTheme.colorScheme.primary, state.progress, Modifier.fillMaxWidth(),
            ) { onNavigate("statistics") }
        }
    }
}

@Composable
private fun MetricRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), content = content)
}

private fun formatMinutes(minutes: Long): String = "${minutes / 60} h ${minutes % 60} min"
