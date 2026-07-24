package com.lifetrack.app.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifetrack.app.presentation.viewmodel.CalendarViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(contentPadding: PaddingValues, viewModel: CalendarViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val locale = Locale("es", "AR")
    ScreenColumn(contentPadding) {
        PageHeader("Calendario", "Frecuencia y constancia de tus registros")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { month = month.minusMonths(1) }) { Icon(Icons.Rounded.ChevronLeft, "Mes anterior") }
            Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)).replaceFirstChar(Char::titlecase), style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = { month = month.plusMonths(1) }) { Icon(Icons.Rounded.ChevronRight, "Mes siguiente") }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            (1..7).forEach { day ->
                Text(
                    DayOfWeek.of(day).getDisplayName(TextStyle.NARROW, locale),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        CalendarGrid(month, selectedDate, state.activityByDate) { selectedDate = it }
        HeatmapLegend()
        val activities = state.activityByDate[selectedDate] ?: 0
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(selectedDate.format(DateTimeFormatter.ofPattern("d 'de' MMMM", locale)), style = MaterialTheme.typography.titleMedium)
                Text(if (activities == 0) "Sin registros para este día" else "$activities registros de seguimiento")
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
        while (size % 7 != 0) add(null)
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        days.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { date ->
                    if (date == null) Box(Modifier.weight(1f).aspectRatio(1f))
                    else {
                        val count = activityByDate[date] ?: 0
                        val isSelected = date == selectedDate
                        val color = when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            count >= 5 -> MaterialTheme.colorScheme.secondary
                            count >= 3 -> MaterialTheme.colorScheme.secondaryContainer
                            count > 0 -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        Box(
                            modifier = Modifier.weight(1f).aspectRatio(1f).clip(MaterialTheme.shapes.small).background(color)
                                .semantics {
                                    contentDescription = "${date.dayOfMonth} de ${date.month.getDisplayName(TextStyle.FULL, Locale("es", "AR"))}, $count registros"
                                    selected = isSelected
                                    role = Role.Button
                                }.clickable { onSelect(date) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(date.dayOfMonth.toString(), color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeatmapLegend() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
        Text("Menos", style = MaterialTheme.typography.labelMedium)
        listOf(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.secondary,
        ).forEach { color -> Box(Modifier.padding(start = 4.dp).clip(MaterialTheme.shapes.extraSmall).background(color).padding(7.dp)) }
        Text("Más", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 4.dp))
    }
}
