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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
                Text("Habitos", style = MaterialTheme.typography.headlineMedium)
                Button(onClick = { showCreateDialog = true }) { Text("Crear") }
            }
        }
        state.error?.let { message ->
            item { ErrorCard(message) }
        }
        if (state.items.isEmpty()) {
            item { EmptyState("Todavia no tenes habitos. Crea uno para comenzar tu seguimiento.") }
        } else {
            items(state.items, key = { it.habit.id }) { item ->
                HabitCard(item, onComplete = { viewModel.complete(item) }, onArchive = { viewModel.archive(item.habit.id) })
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
}

@Composable
private fun HabitCard(item: HabitListItem, onComplete: () -> Unit, onArchive: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(item.habit.name, style = MaterialTheme.typography.titleLarge)
            item.habit.description?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            val unit = when (item.habit.targetType) {
                HabitTargetType.YES_NO -> "completado"
                HabitTargetType.DURATION -> "min"
                HabitTargetType.QUANTITY, HabitTargetType.REPETITIONS -> "registros"
            }
            Text("${item.currentValue.coerceAtMost(item.habit.targetValue)} de ${item.habit.targetValue} $unit")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onComplete, enabled = !item.isComplete) {
                    Text(if (item.isComplete) "Completado" else "Completar")
                }
                OutlinedButton(onClick = onArchive) { Text("Archivar") }
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo habito") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, singleLine = true)
                OutlinedTextField(description, { description = it }, label = { Text("Descripcion opcional") })
                HabitTargetType.entries.forEach { option ->
                    FilterChip(
                        selected = type == option,
                        onClick = { type = option },
                        label = { Text(option.displayName()) },
                    )
                }
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it.filter(Char::isDigit) },
                    label = { Text("Objetivo diario") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            Button(onClick = { onCreate(name, description, type, target.toIntOrNull() ?: 0) }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

private fun HabitTargetType.displayName(): String = when (this) {
    HabitTargetType.YES_NO -> "Si o no"
    HabitTargetType.QUANTITY -> "Cantidad"
    HabitTargetType.DURATION -> "Duracion"
    HabitTargetType.REPETITIONS -> "Repeticiones"
}

@Composable
private fun rememberSaveableState(initial: Boolean) = androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(initial) }

@Composable
private fun rememberSaveableState(initial: String) = androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(initial) }

@Composable
private fun rememberSaveableState(initial: HabitTargetType) = androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(initial) }
