package com.lifetrack.app.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
    viewModel: StatisticsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val preferences by appViewModel.preferences.collectAsStateWithLifecycle()
    ScreenColumn(contentPadding) {
        Text("Resumen", style = MaterialTheme.typography.headlineMedium)
        Text("Ultimos 7 dias", style = MaterialTheme.typography.titleMedium)
        StatisticCard("Promedio de agua", "${state.weeklyWaterAverage} ml por dia")
        StatisticCard("Promedio de sueno", if (state.weeklySleepAverage == 0L) "Sin datos" else formatDuration(state.weeklySleepAverage))
        StatisticCard("Cumplimiento de habitos", "${(state.habitCompletion * 100).toInt()}%")
        StatisticCard("Dias con actividad", "${state.daysWithData} de 7")
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Visualizacion", style = MaterialTheme.typography.titleMedium)
                Text("Los indicadores describen registros locales. No determinan causas ni realizan evaluaciones clinicas.")
                Button(onClick = onOpenCalendar) { Text("Abrir calendario") }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tema", style = MaterialTheme.typography.titleMedium)
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = preferences.themeMode == mode,
                        onClick = { appViewModel.setTheme(mode) },
                        label = { Text(mode.label()) },
                    )
                }
            }
        }
    }
}

@Composable
private fun StatisticCard(title: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "Sistema"
    ThemeMode.LIGHT -> "Claro"
    ThemeMode.DARK -> "Oscuro"
}
