package com.lifetrack.app.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifetrack.app.presentation.viewmodel.HomeViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun HomeScreen(contentPadding: PaddingValues, onNavigate: (String) -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ScreenColumn(contentPadding) {
        Text(state.greeting, style = MaterialTheme.typography.headlineMedium)
        Text(LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Progreso de hoy", style = MaterialTheme.typography.titleLarge)
                Text("${(state.progress * 100).toInt()}% de objetivos completados")
                LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                Text(state.nextAction, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            SummaryCard("Habitos", "${state.completedHabits}/${state.totalHabits}", Modifier.weight(1f)) { onNavigate("habits") }
            SummaryCard("Agua", "${state.waterMl}/${state.waterGoalMl} ml", Modifier.weight(1f)) { onNavigate("water") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { onNavigate("water") }, modifier = Modifier.weight(1f)) { Text("Registrar agua") }
            Button(onClick = { onNavigate("habits") }, modifier = Modifier.weight(1f)) { Text("Registrar habito") }
        }
    }
}

@Composable
private fun SummaryCard(title: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
