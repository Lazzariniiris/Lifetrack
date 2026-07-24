package com.lifetrack.app.domain.repository

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlinx.coroutines.flow.Flow

data class AuthUser(val id: String, val email: String)

enum class AuthError(
    val message: String,
    val retryable: Boolean = false,
    val offline: Boolean = false,
) {
    NotConfigured("La sincronización no está configurada en esta instalación."),
    InvalidCredentials("El correo o la contraseña no son correctos."),
    EmailNotVerified("Confirmá tu correo antes de iniciar sesión."),
    EmailAlreadyRegistered("Ya existe una cuenta con ese correo. Podés iniciar sesión o recuperar la contraseña."),
    RateLimited("Hay demasiados intentos. Esperá unos minutos y volvé a probar.", retryable = true),
    Network("No pudimos conectarnos. Revisá tu conexión e intentá de nuevo.", retryable = true, offline = true),
    Timeout("La solicitud tardó demasiado. Intentá de nuevo.", retryable = true),
    SessionExpired("Tu sesión venció. Iniciá sesión nuevamente."),
    RecoveryLinkInvalid("El enlace de recuperación no es válido o ya venció."),
    InvalidEmail("Ingresá un correo válido."),
    PasswordRequired("Ingresá tu contraseña."),
    WeakPassword("Usá al menos 12 caracteres, con mayúscula, minúscula, número y símbolo."),
    PasswordMismatch("Las contraseñas no coinciden."),
    Unknown("No pudimos completar la solicitud. Intentá de nuevo.", retryable = true),
}

data class AuthFailure(val error: AuthError) {
    val message: String get() = error.message
    val retryable: Boolean get() = error.retryable
    val offline: Boolean get() = error.offline
}

sealed interface AuthResult<out T> {
    data class Success<T>(val value: T) : AuthResult<T>
    data class Failure(val failure: AuthFailure) : AuthResult<Nothing>
}

sealed interface RegistrationOutcome {
    data object SignedIn : RegistrationOutcome
    data class VerificationPending(val email: String) : RegistrationOutcome
}

data class AuthRecoveryTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long,
)

interface AuthRepository {
    val user: Flow<AuthUser?>
    val configured: Boolean
    suspend fun register(email: String, password: String): AuthResult<RegistrationOutcome>
    suspend fun login(email: String, password: String): AuthResult<Unit>
    suspend fun logout(): AuthResult<Unit>
    suspend fun recover(email: String): AuthResult<Unit>
    suspend fun completePasswordRecovery(tokens: AuthRecoveryTokens, password: String): AuthResult<Unit>
    suspend fun validateSession(): AuthResult<AuthUser?>
    suspend fun validAccessToken(): String?
}

private val EMAIL_PATTERN = Regex("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", RegexOption.IGNORE_CASE)

fun normalizeAuthEmail(email: String): String = email.trim().lowercase(Locale.ROOT)

fun emailValidationError(email: String): AuthError? =
    AuthError.InvalidEmail.takeUnless { EMAIL_PATTERN.matches(normalizeAuthEmail(email)) }

fun loginPasswordValidationError(password: String): AuthError? =
    AuthError.PasswordRequired.takeIf { password.isEmpty() }

fun strongPasswordValidationError(password: String): AuthError? = AuthError.WeakPassword.takeUnless {
    password.length >= 12 &&
        password.any(Char::isUpperCase) &&
        password.any(Char::isLowerCase) &&
        password.any(Char::isDigit) &&
        password.any { !it.isLetterOrDigit() }
}

fun passwordConfirmationError(password: String, confirmation: String): AuthError? =
    AuthError.PasswordMismatch.takeUnless { password == confirmation }

fun parseRecoveryLink(link: String): AuthResult<AuthRecoveryTokens> {
    val uri = runCatching { URI(link) }.getOrNull()
        ?: return AuthResult.Failure(AuthFailure(AuthError.RecoveryLinkInvalid))
    if (uri.scheme != "lifetrack" || uri.host != "auth" || uri.path != "/recovery") {
        return AuthResult.Failure(AuthFailure(AuthError.RecoveryLinkInvalid))
    }
    val parameters = decodeParameters(uri.rawQuery) + decodeParameters(uri.rawFragment)
    if (parameters["type"] != "recovery" || parameters["error"] != null || parameters["error_code"] != null) {
        return AuthResult.Failure(AuthFailure(AuthError.RecoveryLinkInvalid))
    }
    val accessToken = parameters["access_token"].orEmpty()
    val refreshToken = parameters["refresh_token"].orEmpty()
    if (accessToken.isBlank() || refreshToken.isBlank()) {
        return AuthResult.Failure(AuthFailure(AuthError.RecoveryLinkInvalid))
    }
    return AuthResult.Success(
        AuthRecoveryTokens(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresInSeconds = parameters["expires_in"]?.toLongOrNull()?.coerceAtLeast(0) ?: 3_600,
        ),
    )
}

private fun decodeParameters(value: String?): Map<String, String> = value
    ?.split('&')
    ?.mapNotNull { entry ->
        val separator = entry.indexOf('=')
        if (separator <= 0) return@mapNotNull null
        runCatching {
            URLDecoder.decode(entry.substring(0, separator), StandardCharsets.UTF_8.name()) to
                URLDecoder.decode(entry.substring(separator + 1), StandardCharsets.UTF_8.name())
        }.getOrNull()
    }
    ?.toMap()
    .orEmpty()
