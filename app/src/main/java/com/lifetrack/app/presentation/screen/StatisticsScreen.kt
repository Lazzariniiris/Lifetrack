package com.lifetrack.app.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifetrack.app.presentation.theme.LifeTrackColors
import com.lifetrack.app.presentation.util.formatDuration
import com.lifetrack.app.presentation.viewmodel.MealStatsViewModel
import com.lifetrack.app.presentation.viewmodel.StatisticsViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private enum class StatisticMetric { WATER, SLEEP, HABITS }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatisticsScreen(
    contentPadding: PaddingValues,
    onOpenCalendar: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel(),
    mealStatsViewModel: MealStatsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val mealStats by mealStatsViewModel.state.collectAsStateWithLifecycle()
    var selectedMetric by rememberSaveable { mutableStateOf(StatisticMetric.WATER) }

    ScreenColumn(contentPadding) {
        PageHeader("Tu evolución", "Tendencias basadas en tus últimos 7 días")
        if (!state.initialized) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            return@ScreenColumn
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            FilterChip(selectedMetric == StatisticMetric.WATER, { selectedMetric = StatisticMetric.WATER }, label = { Text("Agua") })
            FilterChip(selectedMetric == StatisticMetric.SLEEP, { selectedMetric = StatisticMetric.SLEEP }, label = { Text("Sueño") })
            FilterChip(selectedMetric == StatisticMetric.HABITS, { selectedMetric = StatisticMetric.HABITS }, label = { Text("Hábitos") })
        }

        val chartValues = when (selectedMetric) {
            StatisticMetric.WATER -> state.dailyWater.map(Int::toFloat)
            StatisticMetric.SLEEP -> state.dailySleep.map(Long::toFloat)
            StatisticMetric.HABITS -> state.dailyHabitCompletion
        }
        val chartColor = when (selectedMetric) {
            StatisticMetric.WATER -> LifeTrackColors.Water
            StatisticMetric.SLEEP -> LifeTrackColors.Sleep
            StatisticMetric.HABITS -> LifeTrackColors.Habits
        }
        val hasRecordedValue = chartValues.any { it > 0f }
        val headline = when (selectedMetric) {
            StatisticMetric.WATER -> if (hasRecordedValue) "${state.weeklyWaterAverage} ml" else "Sin registros"
            StatisticMetric.SLEEP -> if (hasRecordedValue) formatDuration(state.weeklySleepAverage) else "Sin registros"
            StatisticMetric.HABITS -> "${(state.habitCompletion * 100).toInt()}%"
        }
        val summaryLabel = when (selectedMetric) {
            StatisticMetric.WATER -> "Promedio diario (7 días)"
            StatisticMetric.SLEEP -> "Promedio por día registrado"
            StatisticMetric.HABITS -> "Cumplimiento de los últimos 7 días"
        }
        val calculationNote = when (selectedMetric) {
            StatisticMetric.WATER -> "El promedio divide el total entre los 7 días; los días sin agua registrada aportan 0. Una barra en 0 indica ausencia de registro ese día."
            StatisticMetric.SLEEP -> "El promedio usa el total de sueño de cada día con datos. Una barra en 0 indica que no hay un registro para ese día."
            StatisticMetric.HABITS -> "El porcentaje considera los hábitos activos durante los 7 días; un día sin hábitos completados aporta 0. Si no hay hábitos activos, se muestra 0%."
        }
        val dates = (6 downTo 0).map { LocalDate.now().minusDays(it.toLong()) }
        val valueDescription: (Float) -> String = when (selectedMetric) {
            StatisticMetric.WATER -> { value -> "${value.toInt()} mililitros" }
            StatisticMetric.SLEEP -> { value -> "${value.toInt()} minutos de sueño" }
            StatisticMetric.HABITS -> { value -> "${(value * 100).toInt()} por ciento de hábitos completados" }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(summaryLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(headline, style = MaterialTheme.typography.headlineMedium)
                    }
                    Icon(Icons.Rounded.Insights, contentDescription = null, tint = chartColor)
                }
                WeeklyBarChart(chartValues.reversed(), dates, chartColor, valueDescription)
                Row(modifier = Modifier.fillMaxWidth()) {
                    dates.forEach { date ->
                        Text(
                            date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale("es", "AR")),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            }
        }

        SectionHeader("Indicadores")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InsightCard("Actividad", "${state.daysWithData}/7 días", Icons.Rounded.CheckCircle, Modifier.weight(1f))
            InsightCard("Comidas", mealStats.weekCount.toString(), Icons.Rounded.Restaurant, Modifier.weight(1f))
        }
        StatusCard(
            title = "Cómo se calcula",
            message = "$calculationNote LifeTrack acompaña tu seguimiento y no realiza diagnósticos.",
        )
        Button(onClick = onOpenCalendar, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
            Text("  Ver calendario y heatmap")
        }
    }
}

@Composable
private fun WeeklyBarChart(
    values: List<Float>,
    dates: List<LocalDate>,
    color: Color,
    valueDescription: (Float) -> String,
) {
    val maximum = values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "AR"))
    Row(
        modifier = Modifier.fillMaxWidth().height(112.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        values.forEachIndexed { index, value ->
            val fraction = (value / maximum).coerceIn(0f, 1f)
            Box(
                Modifier
                    .width(22.dp)
                    .height((96.dp * fraction).coerceAtLeast(4.dp))
                    .clip(MaterialTheme.shapes.small)
                    .background(if (value == 0f) MaterialTheme.colorScheme.surfaceVariant else color)
                    .semantics {
                        contentDescription = "${dates[index].format(dateFormatter)}: ${valueDescription(value)}"
                    },
            )
        }
    }
}

@Composable
private fun InsightCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
