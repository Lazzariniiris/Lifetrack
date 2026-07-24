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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifetrack.app.presentation.viewmodel.MealViewModel

@Composable
fun MealsScreen(contentPadding: PaddingValues, onOpenCamera: () -> Unit, viewModel: MealViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ScreenColumn(contentPadding) {
        PageHeader("Alimentación", "Registrá comidas y revisá su estimación nutricional")
        Button(onClick = onOpenCamera, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.CameraAlt, contentDescription = null)
            Text("  Fotografiar comida")
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
        if (meals.isEmpty()) {
            EmptyState("Todavía no registraste comidas. Una foto es suficiente para empezar.")
        } else {
            meals.forEach { meal ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Rounded.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(meal.foods.joinToString { it.name }.ifBlank { "Comida registrada" }, style = MaterialTheme.typography.titleMedium)
                            Text("${meal.nutrition.calories.toInt()} kcal · ${meal.nutrition.proteinG.toInt()} g proteína", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NutritionMetric(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
