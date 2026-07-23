package com.lifetrack.app.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifetrack.app.presentation.viewmodel.AuthViewModel

@Composable
fun AccountScreen(contentPadding: PaddingValues, onBack: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle(); var email by rememberSaveable { mutableStateOf("") }; var password by rememberSaveable { mutableStateOf("") }
    ScreenColumn(contentPadding) {
        Text("Cuenta", style = MaterialTheme.typography.headlineMedium)
        if (!state.configured) ErrorCard("Supabase no esta configurado en este build. El seguimiento local sigue disponible.")
        else if (state.user != null) {
            Text("Sesion activa", style = MaterialTheme.typography.titleLarge); Text(state.user?.email.orEmpty())
            Button(onClick = viewModel::logout, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) { Text("Cerrar sesion") }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(email, { email = it }, label = { Text("Correo") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(password, { password = it }, label = { Text("Contrasena") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
                val validCredentials = email.contains('@') && password.length >= 8
                Button(onClick = { viewModel.login(email, password) }, enabled = validCredentials && !state.loading, modifier = Modifier.fillMaxWidth()) { Text("Iniciar sesion") }
                TextButton(onClick = { viewModel.register(email, password) }, enabled = validCredentials && !state.loading) { Text("Crear cuenta") }
                TextButton(onClick = { viewModel.recover(email) }, enabled = email.isNotBlank() && !state.loading) { Text("Recuperar contrasena") }
            }
        }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        TextButton(onClick = onBack) { Text("Volver") }
    }
}
