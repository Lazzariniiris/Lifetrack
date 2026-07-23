package com.lifetrack.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifetrack.app.domain.model.AppResult
import com.lifetrack.app.domain.repository.AuthRepository
import com.lifetrack.app.domain.repository.AuthUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(val user: AuthUser? = null, val configured: Boolean = false, val loading: Boolean = false, val message: String? = null)
@HiltViewModel
class AuthViewModel @Inject constructor(private val repository: AuthRepository) : ViewModel() {
    private val loading = MutableStateFlow(false); private val message = MutableStateFlow<String?>(null)
    val state: StateFlow<AuthUiState> = combine(repository.user, loading, message) { user, busy, text -> AuthUiState(user, repository.configured, busy, text) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuthUiState(configured = repository.configured))
    fun login(email: String, password: String) = if (valid(email, password)) submit { repository.login(email, password) } else invalid()
    fun register(email: String, password: String) = if (valid(email, password)) submit("Cuenta creada. Revisa tu correo si Supabase solicita confirmacion.") { repository.register(email, password) } else invalid()
    fun recover(email: String) = submit("Revisa tu correo para recuperar la cuenta.") { repository.recover(email) }
    fun logout() = submit { repository.logout() }
    private fun valid(email: String, password: String) = email.contains('@') && password.length >= 8
    private fun invalid() { message.value = "Ingresa un correo valido y una contrasena de al menos 8 caracteres." }
    private fun submit(success: String? = null, action: suspend () -> AppResult<Unit>) = viewModelScope.launch {
        loading.value = true; message.value = null
        message.value = when (val result = action()) { is AppResult.Error -> result.message; is AppResult.Success -> success }
        loading.value = false
    }
}
