package com.lifetrack.app.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifetrack.app.domain.model.AppLanguage
import com.lifetrack.app.domain.model.NutritionPreference
import com.lifetrack.app.domain.model.ThemeMode
import com.lifetrack.app.domain.model.UnitSystem
import com.lifetrack.app.presentation.viewmodel.AppViewModel

@Composable
fun SettingsScreen(contentPadding: PaddingValues, appViewModel: AppViewModel) {
    val preferences by appViewModel.preferences.collectAsStateWithLifecycle()
    ScreenColumn(contentPadding) {
        PageHeader("Configuración", "Preferencias persistentes para tu rutina")
        SettingsSection("Apariencia", Icons.Rounded.DarkMode) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(selected = preferences.themeMode == mode, onClick = { appViewModel.setTheme(mode) }, label = { Text(mode.label()) })
                }
            }
        }
        SettingsSection("Notificaciones", Icons.Rounded.Notifications) {
            Text(if (preferences.waterRemindersEnabled) "Recordatorios de agua activos" else "Recordatorios de agua desactivados")
            Text("El objetivo y horario de hidratación se configuran desde Agua.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Resultados de comidas")
                Switch(
                    checked = preferences.mealAnalysisNotificationsEnabled,
                    onCheckedChange = { appViewModel.updateAppSettings(preferences.language, preferences.unitSystem, preferences.nutritionPreference, it) },
                )
            }
        }
        SettingsSection("Idioma y unidades", Icons.Rounded.Straighten) {
            Text("Idioma", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AppLanguage.entries.forEach { language ->
                    FilterChip(
                        selected = preferences.language == language,
                        onClick = { appViewModel.updateAppSettings(language, preferences.unitSystem, preferences.nutritionPreference, preferences.mealAnalysisNotificationsEnabled) },
                        label = { Text(language.label()) },
                    )
                }
            }
            Text("Unidades", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                UnitSystem.entries.forEach { units ->
                    FilterChip(
                        selected = preferences.unitSystem == units,
                        onClick = { appViewModel.updateAppSettings(preferences.language, units, preferences.nutritionPreference, preferences.mealAnalysisNotificationsEnabled) },
                        label = { Text(if (units == UnitSystem.METRIC) "Métricas" else "Imperiales") },
                    )
                }
            }
        }
        SettingsSection("Preferencia nutricional", Icons.Rounded.Restaurant) {
            NutritionPreference.entries.forEach { nutrition ->
                FilterChip(
                    selected = preferences.nutritionPreference == nutrition,
                    onClick = { appViewModel.updateAppSettings(preferences.language, preferences.unitSystem, nutrition, preferences.mealAnalysisNotificationsEnabled) },
                    label = { Text(nutrition.label()) },
                )
            }
        }
        SettingsSection("Privacidad", Icons.Rounded.Security) {
            Text("Hábitos, agua y sueño permanecen en este dispositivo. Las fotos de comidas se guardan en un bucket privado y se eliminan al borrar el registro.")
        }
    }
}

@Composable
private fun SettingsSection(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleLarge)
            }
            content()
        }
    }
}

private fun ThemeMode.label() = when (this) { ThemeMode.SYSTEM -> "Sistema"; ThemeMode.LIGHT -> "Claro"; ThemeMode.DARK -> "Oscuro" }
private fun AppLanguage.label() = when (this) { AppLanguage.SYSTEM -> "Sistema"; AppLanguage.SPANISH -> "Español"; AppLanguage.ENGLISH -> "English" }
private fun NutritionPreference.label() = when (this) { NutritionPreference.NONE -> "Sin preferencia"; NutritionPreference.VEGETARIAN -> "Vegetariana"; NutritionPreference.VEGAN -> "Vegana"; NutritionPreference.LOW_SODIUM -> "Baja en sodio" }
