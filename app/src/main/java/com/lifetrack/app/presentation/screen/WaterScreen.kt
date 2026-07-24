package com.lifetrack.app.presentation.screen

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifetrack.app.presentation.util.formatDateTime
import com.lifetrack.app.presentation.viewmodel.WaterViewModel

@Composable
fun WaterScreen(contentPadding: PaddingValues, viewModel: WaterViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showSettings by rememberSaveableBoolean(false)
    var showCustomAdd by rememberSaveableBoolean(false)
    var entryToDelete by rememberSaveableString("")
    var pendingReminderSettings by rememberReminderSettings()
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pendingReminderSettings?.let { settings ->
            viewModel.updateSettings(settings.goalMl, settings.quickAddMl, granted)
            pendingReminderSettings = null
        }
    }
    val progress = (state.consumedMl.toFloat() / state.preferences.waterGoalMl).coerceIn(0f, 1f)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp, contentPadding.calculateTopPadding() + 16.dp, 20.dp, contentPadding.calculateBottomPadding() + 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    PageHeader("Hidratación", "Cada vaso te acerca a tu objetivo")
                }
                TextButton(onClick = { showSettings = true }) {
                    Icon(Icons.Rounded.Settings, contentDescription = null)
                    Text("  Ajustes")
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            ) {
                Row(modifier = Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    androidx.compose.foundation.layout.Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                        CircularProgressIndicator(progress = { progress }, color = MaterialTheme.colorScheme.tertiary, trackColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.16f), strokeWidth = 7.dp)
                        Icon(Icons.Default.WaterDrop, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.weight(1f)) {
                        Text("${state.consumedMl} ml", style = MaterialTheme.typography.titleLarge)
                        Text("de ${state.preferences.waterGoalMl} ml hoy", style = MaterialTheme.typography.bodyMedium)
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.tertiary, trackColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.18f))
                        Text("Restan ${(state.preferences.waterGoalMl - state.consumedMl).coerceAtLeast(0)} ml", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(state.preferences.waterQuickAddMl, 150, 250, 500).distinct().take(3).forEach { amount ->
                        Button(onClick = { viewModel.add(amount) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 4.dp)) { Text("+$amount") }
                    }
                }
                OutlinedButton(onClick = { showCustomAdd = true }, modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) { Text("Otra cantidad") }
            }
        }
        state.error?.let { message -> item { ErrorCard(message) } }
        item { SectionHeader("Historial reciente") }
        if (!state.initialized) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        } else if (state.entries.isEmpty()) {
            item { EmptyState("Todavía no hay registros de agua para mostrar.") }
        } else {
            items(state.entries, key = { it.id }) { entry ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text("${entry.amountMl} ml"); Text(formatDateTime(entry.loggedAt), style = MaterialTheme.typography.bodySmall) }
                        TextButton(onClick = { entryToDelete = entry.id }) { Text("Eliminar") }
                    }
                }
            }
        }
    }
    if (showCustomAdd) {
        AmountDialog(
            onDismiss = { showCustomAdd = false },
            onAdd = { amount -> viewModel.add(amount); showCustomAdd = false },
        )
    }
    if (showSettings) {
        WaterSettingsDialog(
            initialGoal = state.preferences.waterGoalMl,
            initialQuickAdd = state.preferences.waterQuickAddMl,
            initialReminders = state.preferences.waterRemindersEnabled,
            onDismiss = { showSettings = false },
            onSave = { goal, quickAdd, reminders ->
                val permissionMissing = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
                if (reminders && permissionMissing) {
                    pendingReminderSettings = ReminderSettings(goal, quickAdd)
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    viewModel.updateSettings(goal, quickAdd, reminders)
                }
                showSettings = false
            },
        )
    }
    if (entryToDelete.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { entryToDelete = "" },
            title = { Text("Eliminar registro de agua") },
            text = { Text("Este registro se quitará del total y del historial.") },
            confirmButton = { Button(onClick = { viewModel.delete(entryToDelete); entryToDelete = "" }) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { entryToDelete = "" }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun AmountDialog(onDismiss: () -> Unit, onAdd: (Int) -> Unit) {
    var amount by rememberSaveableString("250")
    var attempted by rememberSaveableBoolean(false)
    val parsed = amount.toIntOrNull()
    val valid = parsed != null && parsed in 1..2_000
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar agua") },
        text = {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter(Char::isDigit) },
                label = { Text("Cantidad en ml") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = attempted && !valid,
                supportingText = { if (attempted && !valid) Text("Ingresá una cantidad entre 1 y 2.000 ml") },
            )
        },
        confirmButton = { Button(onClick = { attempted = true; if (valid) onAdd(parsed!!) }) { Text("Registrar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun WaterSettingsDialog(
    initialGoal: Int,
    initialQuickAdd: Int,
    initialReminders: Boolean,
    onDismiss: () -> Unit,
    onSave: (Int, Int, Boolean) -> Unit,
) {
    var goal by rememberSaveableString(initialGoal.toString())
    var quickAdd by rememberSaveableString(initialQuickAdd.toString())
    var reminders by rememberSaveableBoolean(initialReminders)
    var attempted by rememberSaveableBoolean(false)
    val parsedGoal = goal.toIntOrNull()
    val parsedQuickAdd = quickAdd.toIntOrNull()
    val validGoal = parsedGoal != null && parsedGoal in 250..10_000
    val validQuickAdd = parsedQuickAdd != null && parsedQuickAdd in 50..1_000
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajustes de hidratación") },
        text = {
            Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(goal, { goal = it.filter(Char::isDigit) }, label = { Text("Objetivo diario (ml)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = attempted && !validGoal, supportingText = { if (attempted && !validGoal) Text("Entre 250 y 10.000 ml") })
                OutlinedTextField(quickAdd, { quickAdd = it.filter(Char::isDigit) }, label = { Text("Registro rápido (ml)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = attempted && !validQuickAdd, supportingText = { if (attempted && !validQuickAdd) Text("Entre 50 y 1.000 ml") })
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Recordatorios adaptativos")
                    Switch(checked = reminders, onCheckedChange = { reminders = it })
                }
                Text("Se ajustan al progreso y a tu último registro. Android puede limitar la entrega para cuidar la batería.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { Button(onClick = { attempted = true; if (validGoal && validQuickAdd) onSave(parsedGoal!!, parsedQuickAdd!!, reminders) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun rememberSaveableBoolean(initial: Boolean) = androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(initial) }

@Composable
private fun rememberSaveableString(initial: String) = androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(initial) }

private data class ReminderSettings(val goalMl: Int, val quickAddMl: Int)

@Composable
private fun rememberReminderSettings() = remember { mutableStateOf<ReminderSettings?>(null) }
