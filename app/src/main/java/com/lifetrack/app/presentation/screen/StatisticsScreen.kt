package com.lifetrack.app.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifetrack.app.domain.model.ThemeMode
import com.lifetrack.app.presentation.util.formatDuration
import com.lifetrack.app.presentation.viewmodel.AppViewModel
import com.lifetrack.app.presentation.viewmodel.StatisticsViewModel

@Composable
fun StatisticsScreen(
    contentPadding: PaddingValues,
    appViewModel: AppViewModel,
    onOpenCalendar: () -> Unit,
    onOpenAbout: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val preferences by appViewModel.preferences.collectAsStateWithLifecycle()
    ScreenColumn(contentPadding) {
        Text("Resumen", style = MaterialTheme.typography.headlineMedium)
        Text("Una mirada clara a tus ultimos 7 dias", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        StatisticCard("Promedio de agua", "${state.weeklyWaterAverage} ml por dia")
        StatisticCard("Promedio de sueno", if (state.weeklySleepAverage == 0L) "Sin datos" else formatDuration(state.weeklySleepAverage))
        StatisticCard("Cumplimiento de habitos", "${(state.habitCompletion * 100).toInt()}%")
        StatisticCard("Dias con actividad", "${state.daysWithData} de 7")
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Text("Visualizacion", style = MaterialTheme.typography.titleMedium)
                }
                Text("Los indicadores describen registros locales. No determinan causas ni realizan evaluaciones clinicas.")
                Button(onClick = onOpenCalendar) { Text("Abrir calendario") }
            }
        }
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.Icon(Icons.Default.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Tema", style = MaterialTheme.typography.titleMedium)
                }
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = preferences.themeMode == mode,
                        onClick = { appViewModel.setTheme(mode) },
                        label = { Text(mode.label()) },
                    )
                }
            }
        }
        Button(onClick = onOpenAbout, modifier = Modifier.fillMaxWidth()) { Text("Acerca de LifeTrack") }
    }
}

@Composable
private fun StatisticCard(title: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "Sistema"
    ThemeMode.LIGHT -> "Claro"
    ThemeMode.DARK -> "Oscuro"
}
