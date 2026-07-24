package com.lifetrack.app.data.repository

import com.lifetrack.app.domain.repository.AuthError
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthErrorMappingTest {
    @Test
    fun `login does not distinguish a missing user from wrong credentials`() {
        assertEquals(
            AuthError.InvalidCredentials,
            mapAuthError(400, null, "User not found", AuthOperation.Login),
        )
        assertEquals(
            AuthError.InvalidCredentials,
            mapAuthError(400, "invalid_credentials", "Invalid login credentials", AuthOperation.Login),
        )
    }

    @Test
    fun `known Supabase errors map to controlled errors`() {
        assertEquals(
            AuthError.EmailNotVerified,
            mapAuthError(400, "email_not_confirmed", "technical text", AuthOperation.Login),
        )
        assertEquals(
            AuthError.EmailAlreadyRegistered,
            mapAuthError(422, "user_already_exists", "technical text", AuthOperation.Register),
        )
        assertEquals(
            AuthError.RateLimited,
            mapAuthError(429, null, "technical text", AuthOperation.Login),
        )
    }

    @Test
    fun `unknown server details never become a user message`() {
        val error = mapAuthError(500, "database_error", "stack trace and host", AuthOperation.Register)

        assertEquals(AuthError.Unknown, error)
        assertEquals("No pudimos completar la solicitud. Intentá de nuevo.", error.message)
    }
}
