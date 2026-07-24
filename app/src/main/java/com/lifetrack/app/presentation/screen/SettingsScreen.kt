package com.lifetrack.app.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifetrack.app.domain.model.ThemeMode
import com.lifetrack.app.presentation.viewmodel.AppViewModel

@Composable
fun SettingsScreen(contentPadding: PaddingValues, appViewModel: AppViewModel) {
    val preferences by appViewModel.preferences.collectAsStateWithLifecycle()
    ScreenColumn(contentPadding) {
        PageHeader("Configuración", "Personalizá LifeTrack para tu rutina")
        SettingsSection("Apariencia", Icons.Rounded.DarkMode) {
            Text("Tema", style = MaterialTheme.typography.titleMedium)
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = preferences.themeMode == mode,
                        onClick = { appViewModel.setTheme(mode) },
                        label = { Text(mode.label()) },
                    )
                }
            }
        }
        SettingsSection("Notificaciones", Icons.Rounded.Notifications) {
            Text(
                if (preferences.waterRemindersEnabled) "Recordatorios de agua activos" else "Recordatorios de agua desactivados",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text("Podés ajustar el objetivo y los recordatorios desde Agua.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SettingsSection("Unidades y región", Icons.Rounded.Straighten) {
            ListItem(headlineContent = { Text("Sistema métrico") }, supportingContent = { Text("Mililitros, gramos y kilocalorías") })
        }
        SettingsSection("Privacidad", Icons.Rounded.Security) {
            Text("Los hábitos, el agua y el sueño se guardan localmente. Las fotos solo se conservan mientras esperan análisis y se eliminan al completarlo.")
        }
    }
}

@Composable
private fun SettingsSection(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleLarge)
            }
            content()
        }
    }
}

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "Sistema"
    ThemeMode.LIGHT -> "Claro"
    ThemeMode.DARK -> "Oscuro"
}
