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

@Composable
fun MealsScreen(contentPadding: PaddingValues, onOpenCamera: () -> Unit, viewModel: MealViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var newestFirst by rememberSaveable { mutableStateOf(true) }
    ScreenColumn(contentPadding) {
        PageHeader("Alimentación", "Registrá comidas y revisá su estimación nutricional")
        Button(onClick = onOpenCamera, enabled = state.ownerUserId != null, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.CameraAlt, contentDescription = null)
            Text("  Fotografiar comida")
        }
        if (state.ownerUserId == null) EmptyState("Iniciá sesión desde Perfil para analizar y sincronizar comidas de forma privada.")
        if (state.queue.isNotEmpty()) {
            SectionHeader("Procesamiento")
            state.queue.filter { it.status != MealQueueStatus.READY }.forEach { item ->
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
                            TextButton(onClick = { viewModel.removeQueued(item.id) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = null)
                                Text(" Eliminar")
                            }
                        }
                    }
                }
            }
        }
        SectionHeader("Resumen")
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
        val filteredMeals = meals.filter { meal -> meal.foods.any { query.isBlank() || it.name.contains(query, ignoreCase = true) } }
            .let { if (newestFirst) it else it.reversed() }
        if (filteredMeals.isEmpty()) {
            EmptyState("Todavía no registraste comidas. Una foto es suficiente para empezar.")
        } else {
            filteredMeals.forEach { meal ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(meal.foods.joinToString { it.name }.ifBlank { "Comida registrada" }, style = MaterialTheme.typography.titleMedium)
                            Text("${meal.nutrition.calories.toInt()} kcal · ${meal.nutrition.proteinG.toInt()} g proteína", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Confianza ${(meal.confidence * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                        }
                        meal.id?.let { id ->
                            TextButton(onClick = { viewModel.deleteMeal(id) }) { Icon(Icons.Rounded.Delete, "Eliminar comida") }
                        }
                    }
                }
            }
        }
        state.error?.let { ErrorCard(it) }
    }
}

@Composable
private fun NutritionMetric(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
