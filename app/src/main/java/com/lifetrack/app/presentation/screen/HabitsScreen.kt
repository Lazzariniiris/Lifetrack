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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifetrack.app.domain.model.HabitTargetType
import com.lifetrack.app.presentation.viewmodel.HabitListItem
import com.lifetrack.app.presentation.viewmodel.HabitsViewModel

@Composable
fun HabitsScreen(contentPadding: PaddingValues, viewModel: HabitsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateDialog by rememberSaveableState(false)
    var habitToArchive by rememberSaveableState("")
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                PageHeader("Hábitos", "Pequeñas acciones, progreso constante")
                FilledTonalButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("  Nuevo")
                }
            }
        }
        state.error?.let { message ->
            item { ErrorCard(message) }
        }
        if (state.items.isEmpty()) {
            item { EmptyState("Todavía no tenés hábitos. Creá uno para empezar tu seguimiento.") }
        } else {
            items(state.items, key = { it.habit.id }) { item ->
                HabitCard(item, onComplete = { viewModel.complete(item) }, onArchive = { habitToArchive = item.habit.id })
            }
        }
    }
    if (showCreateDialog) {
        CreateHabitDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, description, type, target ->
                viewModel.createHabit(name, description, type, target)
                showCreateDialog = false
            },
        )
    }
    if (habitToArchive.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { habitToArchive = "" },
            icon = { Icon(Icons.Rounded.Archive, contentDescription = null) },
            title = { Text("Archivar hábito") },
            text = { Text("Dejará de aparecer en tu lista diaria. Tus registros anteriores se conservarán.") },
            confirmButton = { Button(onClick = { viewModel.archive(habitToArchive); habitToArchive = "" }) { Text("Archivar") } },
            dismissButton = { TextButton(onClick = { habitToArchive = "" }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun HabitCard(item: HabitListItem, onComplete: () -> Unit, onArchive: () -> Unit) {
    val progress = (item.currentValue.toFloat() / item.habit.targetValue).coerceIn(0f, 1f)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isComplete) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (item.isComplete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                )
                Column {
                    Text(item.habit.name, style = MaterialTheme.typography.titleMedium)
                    item.habit.description?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            val unit = when (item.habit.targetType) {
                HabitTargetType.YES_NO -> "objetivo"
                HabitTargetType.DURATION -> "min"
                HabitTargetType.QUANTITY -> "unidades"
                HabitTargetType.REPETITIONS -> "repeticiones"
            }
            Text("${item.currentValue.coerceAtMost(item.habit.targetValue)} de ${item.habit.targetValue} $unit", style = MaterialTheme.typography.bodyMedium)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onComplete, enabled = !item.isComplete) {
                    Text(if (item.isComplete) "Completado" else "Completar")
                }
                TextButton(onClick = onArchive) {
                    Icon(Icons.Rounded.Archive, contentDescription = null)
                    Text("  Archivar")
                }
            }
        }
    }
}

@Composable
private fun CreateHabitDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, HabitTargetType, Int) -> Unit,
) {
    var name by rememberSaveableState("")
    var description by rememberSaveableState("")
    var target by rememberSaveableState("1")
    var type by rememberSaveableState(HabitTargetType.YES_NO)
    var attempted by rememberSaveableState(false)
    val parsedTarget = target.toIntOrNull()
    val validName = name.trim().isNotEmpty()
    val validTarget = type == HabitTargetType.YES_NO || parsedTarget in 1..10_000
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo hábito") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, singleLine = true, isError = attempted && !validName, supportingText = { if (attempted && !validName) Text("Ingresá un nombre") })
                OutlinedTextField(description, { description = it }, label = { Text("Descripción opcional") })
                HabitTargetType.entries.forEach { option ->
                    FilterChip(
                        selected = type == option,
                        onClick = { type = option; if (option == HabitTargetType.YES_NO) target = "1" },
                        label = { Text(option.displayName()) },
                    )
                }
                if (type != HabitTargetType.YES_NO) {
                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it.filter(Char::isDigit) },
                        label = { Text("Objetivo diario") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = attempted && !validTarget,
                        supportingText = { if (attempted && !validTarget) Text("Elegí un valor entre 1 y 10.000") },
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                attempted = true
                if (validName && validTarget) onCreate(name, description, type, if (type == HabitTargetType.YES_NO) 1 else parsedTarget!!)
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

private fun HabitTargetType.displayName(): String = when (this) {
    HabitTargetType.YES_NO -> "Sí o no"
    HabitTargetType.QUANTITY -> "Cantidad"
    HabitTargetType.DURATION -> "Duración"
    HabitTargetType.REPETITIONS -> "Repeticiones"
}

@Composable
private fun rememberSaveableState(initial: Boolean) = androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(initial) }

@Composable
private fun rememberSaveableState(initial: String) = androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(initial) }

@Composable
private fun rememberSaveableState(initial: HabitTargetType) = androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(initial) }
