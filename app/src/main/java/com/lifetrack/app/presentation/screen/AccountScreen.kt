package com.lifetrack.app.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifetrack.app.domain.repository.emailValidationError
import com.lifetrack.app.domain.repository.loginPasswordValidationError
import com.lifetrack.app.domain.repository.passwordConfirmationError
import com.lifetrack.app.domain.repository.strongPasswordValidationError
import com.lifetrack.app.presentation.viewmodel.AuthUiState
import com.lifetrack.app.presentation.viewmodel.AuthViewModel
import com.lifetrack.app.presentation.viewmodel.ProfileViewModel
import com.lifetrack.app.domain.repository.ProfileUpdate

@Composable
fun ProfileScreen(
    contentPadding: PaddingValues,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    recoveryLink: String? = null,
    onRecoveryLinkConsumed: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val profileState by profileViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(recoveryLink) {
        recoveryLink?.let {
            viewModel.handleRecoveryLink(it)
            onRecoveryLinkConsumed()
        }
    }
    LaunchedEffect(state.user?.id) {
        if (state.user != null) profileViewModel.load()
    }

    ScreenColumn(contentPadding) {
        PageHeader("Tu perfil", "Objetivos, cuenta y preferencias en un solo lugar")

        Card(modifier = Modifier.fillMaxWidth()) {
            when {
                !state.configured -> UnconfiguredAccount()
                state.recoveryMode -> PasswordRecoveryForm(state, viewModel)
                state.user != null -> SignedInAccount(state, viewModel)
                else -> AuthForm(state, viewModel)
            }
        }

        state.verificationPendingEmail?.let { email ->
            Card(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text("Verificación pendiente") },
                    supportingContent = {
                        Text("Enviamos un enlace a $email. Confirmá el correo antes de iniciar sesión.")
                    },
                    leadingContent = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                )
            }
        }

        AuthStatus(state, viewModel::retry)

        if (state.user != null) ProfileDetails(profileState, profileViewModel)

        SectionHeader("Preferencias")
        Card(modifier = Modifier.fillMaxWidth()) {
            ProfileLink("Configuración", "Tema, recordatorios y privacidad", Icons.Rounded.Settings, onOpenSettings)
            HorizontalDivider()
            ProfileLink("Acerca de LifeTrack", "Versión, privacidad y alcance", Icons.Rounded.Info, onOpenAbout)
        }
    }
}

@Composable
private fun UnconfiguredAccount() {
    ListItem(
        headlineContent = { Text("Sincronización no configurada") },
        supportingContent = {
            Text("Faltan SUPABASE_URL o SUPABASE_ANON_KEY. Tus registros locales siguen disponibles en este dispositivo.")
        },
        leadingContent = { Icon(Icons.Rounded.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
    )
}

@Composable
private fun SignedInAccount(state: AuthUiState, viewModel: AuthViewModel) {
    ListItem(
        headlineContent = { Text(state.user?.email.orEmpty()) },
        supportingContent = { Text("Las comidas se sincronizan de forma privada. Hábitos, agua y sueño permanecen en este dispositivo.") },
        leadingContent = { Icon(Icons.Rounded.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
    )
    HorizontalDivider()
    TextButton(onClick = viewModel::logout, enabled = !state.loading) { Text("Cerrar sesión") }
}

@Composable
private fun AuthForm(state: AuthUiState, viewModel: AuthViewModel) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    var registering by rememberSaveable { mutableStateOf(false) }
    var validationRequested by rememberSaveable { mutableStateOf(false) }
    val emailError = emailValidationError(email)?.message.takeIf { validationRequested }
    val passwordError = (
        if (registering) strongPasswordValidationError(password) else loginPasswordValidationError(password)
        )?.message.takeIf { validationRequested }
    val confirmationError = passwordConfirmationError(password, confirmation)?.message
        .takeIf { registering && validationRequested }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(if (registering) "Creá tu cuenta" else "Sincronizá tus datos") },
            supportingContent = {
                Text(
                    if (registering) "Usá una contraseña fuerte y confirmala."
                    else "Iniciá sesión para analizar comidas y recuperar su historial.",
                )
            },
            leadingContent = { Icon(Icons.Rounded.Lock, contentDescription = null) },
        )
        AuthTextField(
            value = email,
            onValueChange = { email = it },
            label = "Correo",
            errorMessage = emailError,
            keyboardType = KeyboardType.Email,
        )
        AuthTextField(
            value = password,
            onValueChange = { password = it },
            label = "Contraseña",
            errorMessage = passwordError,
            keyboardType = KeyboardType.Password,
            password = true,
        )
        if (registering) {
            AuthTextField(
                value = confirmation,
                onValueChange = { confirmation = it },
                label = "Confirmar contraseña",
                errorMessage = confirmationError,
                keyboardType = KeyboardType.Password,
                password = true,
                imeAction = ImeAction.Done,
            )
        }
        Button(
            onClick = {
                validationRequested = true
                val valid = emailValidationError(email) == null &&
                    (if (registering) strongPasswordValidationError(password) else loginPasswordValidationError(password)) == null &&
                    (!registering || passwordConfirmationError(password, confirmation) == null)
                if (valid && registering) viewModel.register(email, password, confirmation)
                else if (valid) viewModel.login(email, password)
            },
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            if (state.loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            else Text(if (registering) "Crear cuenta" else "Iniciar sesión")
        }
        TextButton(
            onClick = {
                registering = !registering
                validationRequested = false
            },
            enabled = !state.loading,
        ) {
            Text(if (registering) "Ya tengo una cuenta" else "Crear una cuenta")
        }
        if (!registering) {
            TextButton(
                onClick = {
                    validationRequested = true
                    if (emailValidationError(email) == null) viewModel.recover(email)
                },
                enabled = !state.loading,
            ) { Text("Recuperar contraseña") }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ProfileDetails(state: com.lifetrack.app.presentation.viewmodel.ProfileUiState, viewModel: ProfileViewModel) {
    val profile = state.profile
    if (state.loading && profile == null) {
        CircularProgressIndicator()
        return
    }
    if (profile == null) {
        state.error?.let { ErrorCard(it) }
        TextButton(onClick = viewModel::load) { Text("Reintentar perfil") }
        return
    }
    var name by rememberSaveable(profile.id, profile.displayName) { mutableStateOf(profile.displayName) }
    var goal by rememberSaveable(profile.id, profile.healthGoal) { mutableStateOf(profile.healthGoal.orEmpty()) }
    var weight by rememberSaveable(profile.id, profile.weightKg) { mutableStateOf(profile.weightKg?.toString().orEmpty()) }
    var height by rememberSaveable(profile.id, profile.heightCm) { mutableStateOf(profile.heightCm?.toString().orEmpty()) }
    var calorieGoal by rememberSaveable(profile.id, profile.dailyCalorieGoal) { mutableStateOf(profile.dailyCalorieGoal?.toString().orEmpty()) }
    var activity by rememberSaveable(profile.id, profile.activityLevel) { mutableStateOf(profile.activityLevel ?: "moderate") }
    var validationRequested by rememberSaveable(profile.id) { mutableStateOf(false) }
    val parsedWeight = weight.replace(',', '.').toDoubleOrNull()
    val parsedHeight = height.replace(',', '.').toDoubleOrNull()
    val parsedCalories = calorieGoal.toIntOrNull()
    val validName = name.trim().length in 1..80
    val validGoal = goal.length <= 200
    val validWeight = weight.isBlank() || (parsedWeight != null && parsedWeight in 20.0..500.0)
    val validHeight = height.isBlank() || (parsedHeight != null && parsedHeight in 80.0..250.0)
    val validCalories = calorieGoal.isBlank() || parsedCalories in 500..10_000
    val valid = validName && validGoal && validWeight && validHeight && validCalories

    SectionHeader("Datos de salud")
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Miembro desde ${profile.createdAt.take(10)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(), singleLine = true, isError = validationRequested && !validName, supportingText = { if (validationRequested && !validName) Text("Ingresá entre 1 y 80 caracteres") })
            OutlinedTextField(goal, { goal = it }, label = { Text("Objetivo") }, modifier = Modifier.fillMaxWidth(), isError = validationRequested && !validGoal, supportingText = { if (validationRequested && !validGoal) Text("Máximo 200 caracteres") })
            OutlinedTextField(weight, { weight = it.filter { char -> char.isDigit() || char == '.' || char == ',' } }, label = { Text("Peso (kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), singleLine = true, isError = validationRequested && !validWeight, supportingText = { if (validationRequested && !validWeight) Text("Entre 20 y 500 kg") })
            OutlinedTextField(height, { height = it.filter { char -> char.isDigit() || char == '.' || char == ',' } }, label = { Text("Altura (cm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), singleLine = true, isError = validationRequested && !validHeight, supportingText = { if (validationRequested && !validHeight) Text("Entre 80 y 250 cm") })
            OutlinedTextField(calorieGoal, { calorieGoal = it.filter(Char::isDigit) }, label = { Text("Objetivo calórico diario") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true, isError = validationRequested && !validCalories, supportingText = { if (validationRequested && !validCalories) Text("Entre 500 y 10.000 kcal") })
            Text("Actividad física", style = MaterialTheme.typography.labelLarge)
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("sedentary" to "Sedentaria", "light" to "Ligera", "moderate" to "Moderada", "active" to "Activa", "very_active" to "Muy activa").forEach { (value, label) ->
                    FilterChip(selected = activity == value, onClick = { activity = value }, label = { Text(label) })
                }
            }
            Button(
                onClick = {
                    validationRequested = true
                    if (valid) viewModel.save(
                        ProfileUpdate(
                            displayName = name.trim(),
                            healthGoal = goal,
                            weightKg = parsedWeight,
                            heightCm = parsedHeight,
                            activityLevel = activity,
                            dailyCalorieGoal = calorieGoal.toIntOrNull(),
                        ),
                    )
                },
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.loading) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("  Guardando…")
                } else Text("Guardar perfil")
            }
            state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun PasswordRecoveryForm(state: AuthUiState, viewModel: AuthViewModel) {
    var password by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    var validationRequested by rememberSaveable { mutableStateOf(false) }
    val passwordError = strongPasswordValidationError(password)?.message.takeIf { validationRequested }
    val confirmationError = passwordConfirmationError(password, confirmation)?.message.takeIf { validationRequested }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text("Nueva contraseña") },
            supportingContent = { Text("Debe tener 12 caracteres y combinar mayúsculas, minúsculas, números y símbolos.") },
            leadingContent = { Icon(Icons.Rounded.Lock, contentDescription = null) },
        )
        AuthTextField(
            value = password,
            onValueChange = { password = it },
            label = "Nueva contraseña",
            errorMessage = passwordError,
            keyboardType = KeyboardType.Password,
            password = true,
        )
        AuthTextField(
            value = confirmation,
            onValueChange = { confirmation = it },
            label = "Confirmar nueva contraseña",
            errorMessage = confirmationError,
            keyboardType = KeyboardType.Password,
            password = true,
            imeAction = ImeAction.Done,
        )
        Button(
            onClick = {
                validationRequested = true
                if (
                    strongPasswordValidationError(password) == null &&
                    passwordConfirmationError(password, confirmation) == null
                ) {
                    viewModel.updateRecoveredPassword(password, confirmation)
                }
            },
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            if (state.loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            else Text("Guardar contraseña")
        }
        TextButton(onClick = viewModel::cancelRecovery, enabled = !state.loading) { Text("Cancelar") }
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    errorMessage: String?,
    keyboardType: KeyboardType,
    password: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
) {
    var passwordVisible by rememberSaveable(label) { mutableStateOf(false) }
    val fieldModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .then(if (errorMessage != null) Modifier.semantics { error(errorMessage) } else Modifier)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = errorMessage != null,
        supportingText = errorMessage?.let { message -> { Text(message) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        visualTransformation = if (password && !passwordVisible) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        trailingIcon = if (password) {
            {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña")
                }
            }
        } else null,
        modifier = fieldModifier,
        singleLine = true,
    )
}

@Composable
private fun AuthStatus(state: AuthUiState, onRetry: () -> Unit) {
    state.message?.let {
        Text(
            it,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
    state.error?.let { failure ->
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
        ) {
            Text(
                if (failure.offline) "Sin conexión. ${failure.message}" else failure.message,
                color = MaterialTheme.colorScheme.error,
            )
            if (failure.retryable) TextButton(onClick = onRetry, enabled = !state.loading) { Text("Reintentar") }
        }
    }
}

@Composable
private fun ProfileLink(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = { Icon(Icons.Rounded.ChevronRight, contentDescription = null) },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}
