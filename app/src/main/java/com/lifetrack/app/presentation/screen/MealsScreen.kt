package com.lifetrack.app.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifetrack.app.presentation.viewmodel.MealViewModel
import com.lifetrack.app.domain.repository.MealQueueStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun MealsScreen(contentPadding: PaddingValues, onOpenCamera: () -> Unit, viewModel: MealViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var newestFirst by rememberSaveable { mutableStateOf(true) }
    var queuedToDelete by rememberSaveable { mutableStateOf<String?>(null) }
    var mealToDelete by rememberSaveable { mutableStateOf<String?>(null) }
    ScreenColumn(contentPadding) {
        PageHeader("Alimentación", "Registrá comidas y revisá su estimación nutricional")
        Button(onClick = onOpenCamera, enabled = state.ownerUserId != null, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.CameraAlt, contentDescription = null)
            Text("  Fotografiar comida")
        }
        if (!state.authInitialized) LinearProgressIndicator(Modifier.fillMaxWidth())
        else if (state.ownerUserId == null) EmptyState("Iniciá sesión desde Perfil para analizar y sincronizar comidas de forma privada.")
        if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.notice?.let { StatusCard(it, containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) }
        state.error?.let { ErrorCard(it) }
        val readyItems = state.queue.filter { it.status == MealQueueStatus.READY }
        if (readyItems.isNotEmpty()) {
            Card(onClick = onOpenCamera, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (readyItems.size == 1) "Resultado listo para revisar" else "${readyItems.size} resultados listos", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    Text("Confirmá los alimentos y valores antes de incorporarlos al historial.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Revisar ahora", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        val processingItems = state.queue.filter { it.status != MealQueueStatus.READY }
        if (processingItems.isNotEmpty()) {
            SectionHeader("Procesamiento")
            processingItems.forEach { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            when (item.status) {
                                MealQueueStatus.PENDING -> "Pendiente de análisis"
                                MealQueueStatus.FAILED -> "Necesita tu atención"
                                MealQueueStatus.READY -> "Resultado listo"
                            },
                            style = MaterialTheme.typography.titleMedium,
                        )
                        item.lastError?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (item.status == MealQueueStatus.FAILED) {
                                TextButton(onClick = { viewModel.retryQueued(item.id) }) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                                    Text(" Reintentar")
                                }
                            }
                            TextButton(onClick = { queuedToDelete = item.id }) {
                                Icon(Icons.Rounded.Delete, contentDescription = null)
                                Text(" Eliminar")
                            }
                        }
                    }
                }
            }
        }
        SectionHeader("Todo el historial")
        val meals = state.history
        val calories = meals.sumOf { it.nutrition.calories }.toInt()
        val protein = meals.sumOf { it.nutrition.proteinG }.toInt()
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NutritionMetric(calories.toString(), "kcal")
                NutritionMetric(protein.toString(), "g proteína")
                NutritionMetric(meals.size.toString(), "comidas")
            }
        }
        SectionHeader("Historial")
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Buscar alimento") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(onClick = { newestFirst = !newestFirst }) { Text(if (newestFirst) "Más recientes primero" else "Más antiguos primero") }
        val filteredMeals = meals.filter { meal -> query.isBlank() || meal.foods.any { it.name.contains(query, ignoreCase = true) } }
            .let { if (newestFirst) it else it.reversed() }
        if (filteredMeals.isEmpty()) {
            EmptyState(if (meals.isEmpty()) "Todavía no registraste comidas. Una foto es suficiente para empezar." else "No encontramos comidas que coincidan con «$query».")
        } else {
            filteredMeals.forEach { meal ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(meal.foods.joinToString { it.name }.ifBlank { "Comida registrada" }, style = MaterialTheme.typography.titleMedium)
                            Text("${meal.nutrition.calories.toInt()} kcal · ${meal.nutrition.proteinG.toInt()} g proteína", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Confianza ${(meal.confidence * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                            meal.createdAt?.let { Text(formatMealDate(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                        meal.id?.let { id ->
                            TextButton(onClick = { mealToDelete = id }) { Icon(Icons.Rounded.Delete, "Eliminar comida") }
                        }
                    }
                }
            }
        }
    }
    queuedToDelete?.let { id ->
        DeleteConfirmation(
            title = "Eliminar fotografía pendiente",
            message = "Se eliminarán la copia local, el registro pendiente y la fotografía privada. Esta acción no se puede deshacer.",
            onDismiss = { queuedToDelete = null },
            onConfirm = { queuedToDelete = null; viewModel.removeQueued(id) },
        )
    }
    mealToDelete?.let { id ->
        DeleteConfirmation(
            title = "Eliminar comida",
            message = "Se eliminarán el registro y su fotografía privada de tu cuenta. Esta acción no se puede deshacer.",
            onDismiss = { mealToDelete = null },
            onConfirm = { mealToDelete = null; viewModel.deleteMeal(id) },
        )
    }
}

@Composable
private fun DeleteConfirmation(title: String, message: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { Button(onClick = onConfirm) { Text("Eliminar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

private fun formatMealDate(value: String): String = runCatching {
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(Locale("es", "AR"))
        .format(Instant.parse(value).atZone(ZoneId.systemDefault()))
}.getOrDefault(value.take(16).replace('T', ' '))

@Composable
private fun NutritionMetric(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
