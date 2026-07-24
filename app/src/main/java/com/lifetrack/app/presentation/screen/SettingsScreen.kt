package com.lifetrack.app.presentation.screen

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.ContextCompat
import com.lifetrack.app.domain.model.ThemeMode
import com.lifetrack.app.presentation.viewmodel.AppViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(contentPadding: PaddingValues, appViewModel: AppViewModel) {
    val preferences by appViewModel.preferences.collectAsStateWithLifecycle()
    val context = LocalContext.current
    fun notificationPermissionGranted(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
    var mealNotificationPermissionGranted by remember { mutableStateOf(notificationPermissionGranted()) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        mealNotificationPermissionGranted = granted
        appViewModel.updateAppSettings(preferences.language, preferences.unitSystem, preferences.nutritionPreference, granted)
    }
    ScreenColumn(contentPadding) {
        PageHeader("Configuración", "Ajustes disponibles en esta versión")
        SettingsSection("Apariencia", Icons.Rounded.DarkMode) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(selected = preferences.themeMode == mode, onClick = { appViewModel.setTheme(mode) }, label = { Text(mode.label()) })
                }
            }
        }
        SettingsSection("Notificaciones", Icons.Rounded.Notifications) {
            Text(if (preferences.waterRemindersEnabled) "Recordatorios de agua: activados" else "Recordatorios de agua: desactivados")
            Text("Podés cambiar el objetivo y este estado desde Hidratación. El período silencioso actual es 22:00–07:00.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Avisar al finalizar el análisis de una comida", modifier = Modifier.weight(1f).padding(end = 12.dp))
                Switch(
                    checked = preferences.mealAnalysisNotificationsEnabled && mealNotificationPermissionGranted,
                    onCheckedChange = { enabled ->
                        if (enabled && !notificationPermissionGranted() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            mealNotificationPermissionGranted = if (enabled) notificationPermissionGranted() else false
                            appViewModel.updateAppSettings(preferences.language, preferences.unitSystem, preferences.nutritionPreference, enabled)
                        }
                    },
                    modifier = Modifier.semantics { contentDescription = "Aviso al finalizar el análisis de una comida" },
                )
            }
            Text("La entrega de avisos depende del permiso de notificaciones del sistema.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SettingsSection("Privacidad", Icons.Rounded.Security) {
            Text("Los registros de hábitos, agua y sueño se almacenan en este dispositivo.")
            Text("Al solicitar el análisis de una comida, la fotografía se envía a un servicio remoto para procesarla; las comidas guardadas se asocian a tu cuenta.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsSection(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            }
            content()
        }
    }
}

private fun ThemeMode.label() = when (this) { ThemeMode.SYSTEM -> "Sistema"; ThemeMode.LIGHT -> "Claro"; ThemeMode.DARK -> "Oscuro" }
