package com.lifetrack.app.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifetrack.app.presentation.viewmodel.AuthViewModel

@Composable
fun ProfileScreen(
    contentPadding: PaddingValues,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val validCredentials = email.contains('@') && password.length >= 8

    ScreenColumn(contentPadding) {
        PageHeader("Tu perfil", "Objetivos, cuenta y preferencias en un solo lugar")

        Card(modifier = Modifier.fillMaxWidth()) {
            if (!state.configured) {
                ListItem(
                    headlineContent = { Text("Seguimiento local activo") },
                    supportingContent = { Text("Tus registros permanecen disponibles en este dispositivo.") },
                    leadingContent = { Icon(Icons.Rounded.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                )
            } else if (state.user != null) {
                ListItem(
                    headlineContent = { Text(state.user?.email.orEmpty()) },
                    supportingContent = { Text("Cuenta sincronizada") },
                    leadingContent = { Icon(Icons.Rounded.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                )
                HorizontalDivider()
                TextButton(onClick = viewModel::logout, enabled = !state.loading) { Text("Cerrar sesión") }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text("Sincronizá tus datos") },
                        supportingContent = { Text("Iniciá sesión para analizar comidas y mantener una copia segura.") },
                        leadingContent = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                    )
                    OutlinedTextField(
                        email, { email = it }, label = { Text("Correo") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), singleLine = true,
                    )
                    OutlinedTextField(
                        password, { password = it }, label = { Text("Contraseña") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), singleLine = true,
                    )
                    Button(onClick = { viewModel.login(email, password) }, enabled = validCredentials && !state.loading, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        Text("Iniciar sesión")
                    }
                    TextButton(onClick = { viewModel.register(email, password) }, enabled = validCredentials && !state.loading) { Text("Crear cuenta") }
                    TextButton(onClick = { viewModel.recover(email) }, enabled = email.contains('@') && !state.loading) { Text("Recuperar contraseña") }
                }
            }
        }

        state.message?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }

        SectionHeader("Preferencias")
        Card(modifier = Modifier.fillMaxWidth()) {
            ProfileLink("Configuración", "Tema, recordatorios y privacidad", Icons.Rounded.Settings, onOpenSettings)
            HorizontalDivider()
            ProfileLink("Acerca de LifeTrack", "Versión, privacidad y alcance", Icons.Rounded.Info, onOpenAbout)
        }
    }
}

@Composable
private fun ProfileLink(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = { Icon(Icons.Rounded.ChevronRight, contentDescription = null) },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}
