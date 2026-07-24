package com.lifetrack.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifetrack.app.domain.repository.AuthError
import com.lifetrack.app.domain.repository.AuthFailure
import com.lifetrack.app.domain.repository.AuthRecoveryTokens
import com.lifetrack.app.domain.repository.AuthRepository
import com.lifetrack.app.domain.repository.AuthResult
import com.lifetrack.app.domain.repository.AuthUser
import com.lifetrack.app.domain.repository.RegistrationOutcome
import com.lifetrack.app.domain.repository.emailValidationError
import com.lifetrack.app.domain.repository.loginPasswordValidationError
import com.lifetrack.app.domain.repository.parseRecoveryLink
import com.lifetrack.app.domain.repository.passwordConfirmationError
import com.lifetrack.app.domain.repository.strongPasswordValidationError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val user: AuthUser? = null,
    val configured: Boolean = false,
    val loading: Boolean = false,
    val message: String? = null,
    val error: AuthFailure? = null,
    val verificationPendingEmail: String? = null,
    val recoveryMode: Boolean = false,
)

@HiltViewModel
class AuthViewModel @Inject constructor(private val repository: AuthRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(
        AuthUiState(configured = repository.configured, loading = repository.configured),
    )
    val state: StateFlow<AuthUiState> = mutableState.asStateFlow()
    private var retryAction: (() -> Unit)? = null
    private var recoveryTokens: AuthRecoveryTokens? = null

    init {
        viewModelScope.launch {
            repository.user.collect { user -> mutableState.update { it.copy(user = user) } }
        }
        if (repository.configured) validateSession()
    }

    fun login(email: String, password: String) {
        validationFailure(emailValidationError(email) ?: loginPasswordValidationError(password))?.let { return }
        submit(
            retry = { login(email, password) },
            action = { repository.login(email, password) },
        ) { state, _ -> state.copy(message = "Sesión iniciada.", verificationPendingEmail = null) }
    }

    fun register(email: String, password: String, confirmation: String) {
        validationFailure(
            emailValidationError(email)
                ?: strongPasswordValidationError(password)
                ?: passwordConfirmationError(password, confirmation),
        )?.let { return }
        submit(
            retry = { register(email, password, confirmation) },
            action = { repository.register(email, password) },
        ) { state, outcome ->
            when (outcome) {
                RegistrationOutcome.SignedIn -> state.copy(
                    message = "Cuenta creada y sesión iniciada.",
                    verificationPendingEmail = null,
                )
                is RegistrationOutcome.VerificationPending -> state.copy(
                    message = null,
                    verificationPendingEmail = outcome.email,
                )
            }
        }
    }

    fun recover(email: String) {
        validationFailure(emailValidationError(email))?.let { return }
        submit(
            retry = { recover(email) },
            action = { repository.recover(email) },
        ) { state, _ ->
            state.copy(message = "Si existe una cuenta para ese correo, recibirás un enlace para restablecerla.")
        }
    }

    fun handleRecoveryLink(link: String) {
        when (val result = parseRecoveryLink(link)) {
            is AuthResult.Success -> {
                recoveryTokens = result.value
                retryAction = null
                mutableState.update {
                    it.copy(
                        loading = false,
                        recoveryMode = true,
                        message = "Creá una nueva contraseña para completar la recuperación.",
                        error = null,
                    )
                }
            }
            is AuthResult.Failure -> showFailure(result.failure)
        }
    }

    fun updateRecoveredPassword(password: String, confirmation: String) {
        validationFailure(
            strongPasswordValidationError(password) ?: passwordConfirmationError(password, confirmation),
        )?.let { return }
        val tokens = recoveryTokens ?: return showFailure(AuthFailure(AuthError.RecoveryLinkInvalid))
        submit(
            retry = { updateRecoveredPassword(password, confirmation) },
            action = { repository.completePasswordRecovery(tokens, password) },
        ) { state, _ ->
            recoveryTokens = null
            state.copy(recoveryMode = false, message = "Contraseña actualizada y sesión iniciada.")
        }
    }

    fun cancelRecovery() {
        recoveryTokens = null
        retryAction = null
        mutableState.update { it.copy(recoveryMode = false, message = null, error = null) }
    }

    fun logout() {
        submit(retry = ::logout, action = repository::logout) { state, _ ->
            state.copy(message = "Sesión cerrada.", verificationPendingEmail = null)
        }
    }

    fun retry() {
        retryAction?.invoke()
    }

    private fun validateSession() {
        submit(retry = ::validateSession, action = repository::validateSession) { state, _ -> state }
    }

    private fun validationFailure(error: AuthError?): AuthFailure? {
        if (error == null) return null
        val failure = AuthFailure(error)
        showFailure(failure)
        return failure
    }

    private fun showFailure(failure: AuthFailure) {
        if (!failure.retryable) retryAction = null
        mutableState.update { it.copy(loading = false, message = null, error = failure) }
    }

    private fun <T> submit(
        retry: () -> Unit,
        action: suspend () -> AuthResult<T>,
        onSuccess: (AuthUiState, T) -> AuthUiState,
    ) {
        retryAction = retry
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true, message = null, error = null) }
            when (val result = action()) {
                is AuthResult.Failure -> showFailure(result.failure)
                is AuthResult.Success -> {
                    retryAction = null
                    mutableState.update { onSuccess(it.copy(loading = false, error = null), result.value) }
                }
            }
        }
    }
}
