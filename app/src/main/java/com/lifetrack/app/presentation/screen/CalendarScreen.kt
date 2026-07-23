package com.lifetrack.app.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifetrack.app.presentation.viewmodel.CalendarViewModel
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(contentPadding: PaddingValues, onBack: () -> Unit, viewModel: CalendarViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var month by rememberMonth(YearMonth.now())
    var selectedDate by rememberDate(LocalDate.now())
    ScreenColumn(contentPadding) {
        Text("Calendario", style = MaterialTheme.typography.headlineMedium)
        Text("Tu actividad diaria, de un vistazo", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { month = month.minusMonths(1) }) { Text("Anterior") }
            Text(month.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())), style = MaterialTheme.typography.titleLarge)
            Button(onClick = { month = month.plusMonths(1) }) { Text("Siguiente") }
        }
        TextButton(onClick = onBack) { Text("Volver al resumen") }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            (1..7).forEach { day -> Text(DayOfWeek.of(day).getDisplayName(TextStyle.NARROW, Locale.getDefault())) }
        }
        CalendarGrid(month, selectedDate, state.activityByDate, onSelect = { selectedDate = it })
        val activities = state.activityByDate[selectedDate] ?: 0
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(selectedDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.getDefault())), style = MaterialTheme.typography.titleMedium)
                Text(if (activities == 0) "Sin registros para este dia." else "$activities registros de seguimiento.")
                Text("El numero acompana el color para que el estado no dependa solo de la tonalidad.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun CalendarGrid(month: YearMonth, selectedDate: LocalDate, activityByDate: Map<LocalDate, Int>, onSelect: (LocalDate) -> Unit) {
    val firstOffset = month.atDay(1).dayOfWeek.value - 1
    val days = buildList<LocalDate?> {
        repeat(firstOffset) { add(null) }
        repeat(month.lengthOfMonth()) { add(month.atDay(it + 1)) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        days.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                week.forEach { date ->
                    if (date == null) {
                        Text(" ", modifier = Modifier.size(40.dp))
                    } else {
                        val count = activityByDate[date] ?: 0
                        val selected = date == selectedDate
                        Text(
                            text = "${date.dayOfMonth}\n$count",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(if (selected) MaterialTheme.colorScheme.primaryContainer else if (count > 0) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onSelect(date) }
                                .padding(4.dp),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberMonth(initial: YearMonth) = remember { mutableStateOf(initial) }

@Composable
private fun rememberDate(initial: LocalDate) = remember { mutableStateOf(initial) }
