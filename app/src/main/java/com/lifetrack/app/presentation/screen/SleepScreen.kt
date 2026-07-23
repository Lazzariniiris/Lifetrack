package com.lifetrack.app.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifetrack.app.presentation.util.defaultBedtimeInput
import com.lifetrack.app.presentation.util.defaultWakeInput
import com.lifetrack.app.presentation.util.formatDateTime
import com.lifetrack.app.presentation.util.formatDuration
import com.lifetrack.app.presentation.util.parseDateTime
import com.lifetrack.app.presentation.viewmodel.SleepViewModel

@Composable
fun SleepScreen(contentPadding: PaddingValues, viewModel: SleepViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDialog by rememberSaveableBool(false)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp, contentPadding.calculateTopPadding() + 16.dp, 20.dp, contentPadding.calculateBottomPadding() + 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Sueno", style = MaterialTheme.typography.headlineMedium)
                    Text("Conoce tu ritmo de descanso", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = { showDialog = true }) { Text("Registrar") }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Row(modifier = Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Bedtime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Registra tus horarios para observar tu descanso. Es seguimiento personal, no diagnostico.", color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }
        state.error?.let { message -> item { ErrorCard(message) } }
        if (state.entries.isEmpty()) {
            item { EmptyState("No hay sesiones de sueno registradas.") }
        } else {
            items(state.entries, key = { it.id }) { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(formatDuration(entry.durationMinutes), style = MaterialTheme.typography.titleLarge)
                            Text("${formatDateTime(entry.bedtime)} a ${formatDateTime(entry.wakeTime)}", style = MaterialTheme.typography.bodySmall)
                            Text("Calidad percibida: ${entry.quality}/5")
                            entry.notes?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        }
                        TextButton(onClick = { viewModel.delete(entry.id) }) { Text("Eliminar") }
                    }
                }
            }
        }
    }
    if (showDialog) {
        SleepDialog(
            onDismiss = { showDialog = false },
            onSave = { bedtime, wakeTime, quality, notes ->
                viewModel.add(parseDateTime(bedtime), parseDateTime(wakeTime), quality, notes)
                showDialog = false
            },
        )
    }
}

@Composable
private fun SleepDialog(onDismiss: () -> Unit, onSave: (String, String, Int, String) -> Unit) {
    var bedtime by rememberSaveableText(defaultBedtimeInput())
    var wakeTime by rememberSaveableText(defaultWakeInput())
    var quality by rememberSaveableText("3")
    var notes by rememberSaveableText("")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar sueno") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Formato de fecha y hora: dd/MM/aaaa HH:mm")
                OutlinedTextField(bedtime, { bedtime = it }, label = { Text("Acostarse") }, singleLine = true)
                OutlinedTextField(wakeTime, { wakeTime = it }, label = { Text("Levantarse") }, singleLine = true)
                OutlinedTextField(quality, { quality = it.filter(Char::isDigit) }, label = { Text("Calidad percibida (1 a 5)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(notes, { notes = it }, label = { Text("Notas opcionales") })
            }
        },
        confirmButton = { Button(onClick = { onSave(bedtime, wakeTime, quality.toIntOrNull() ?: 0, notes) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun rememberSaveableBool(initial: Boolean) = androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(initial) }

@Composable
private fun rememberSaveableText(initial: String) = androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(initial) }
