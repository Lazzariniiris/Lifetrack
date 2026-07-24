package com.lifetrack.app.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
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
    var savedMonth by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var savedSelectedDate by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val month = YearMonth.parse(savedMonth)
    val selectedDate = LocalDate.parse(savedSelectedDate)
    val locale = Locale("es", "AR")

    fun moveMonth(offset: Long) {
        val nextMonth = month.plusMonths(offset)
        savedMonth = nextMonth.toString()
        savedSelectedDate = nextMonth.atDay(selectedDate.dayOfMonth.coerceAtMost(nextMonth.lengthOfMonth())).toString()
    }

    ScreenColumn(contentPadding) {
        PageHeader("Calendario", "Frecuencia y constancia de tus registros")
        if (!state.initialized) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            return@ScreenColumn
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { moveMonth(-1) }) { Icon(Icons.Rounded.ChevronLeft, "Mes anterior") }
            Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)).replaceFirstChar(Char::titlecase), style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = { moveMonth(1) }) { Icon(Icons.Rounded.ChevronRight, "Mes siguiente") }
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
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
            CalendarGrid(month, selectedDate, state.activityByDate) { savedSelectedDate = it.toString() }
        }
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
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    if (date == null) Box(Modifier.weight(1f).heightIn(min = 48.dp))
                    else {
                        val count = activityByDate[date] ?: 0
                        val isSelected = date == selectedDate
                        val countDescription = if (count == 1) "1 registro" else "$count registros"
                        val (containerColor, contentColor) = when {
                            isSelected -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
                            count >= 5 -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary
                            count >= 3 -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
                            count > 0 -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Box(
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                .clip(MaterialTheme.shapes.small).background(containerColor)
                                .clickable(role = Role.Button, onClickLabel = "Seleccionar fecha") { onSelect(date) }
                                .semantics {
                                    contentDescription = "${date.dayOfMonth} de ${date.month.getDisplayName(TextStyle.FULL, Locale("es", "AR"))}, $countDescription"
                                    selected = isSelected
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(date.dayOfMonth.toString(), color = contentColor)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeatmapLegend() {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Registros por día", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(
                "0" to MaterialTheme.colorScheme.surfaceVariant,
                "1-2" to MaterialTheme.colorScheme.primaryContainer,
                "3-4" to MaterialTheme.colorScheme.secondaryContainer,
                "5+" to MaterialTheme.colorScheme.secondary,
            ).forEach { (label, color) ->
                Row(
                    modifier = Modifier.semantics(mergeDescendants = true) {},
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(14.dp).clip(MaterialTheme.shapes.extraSmall).background(color))
                    Text(label, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
