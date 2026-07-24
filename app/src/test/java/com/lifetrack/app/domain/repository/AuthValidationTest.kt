package com.lifetrack.app.domain.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidationTest {
    @Test
    fun `email is trimmed and normalized`() {
        assertEquals("person@example.com", normalizeAuthEmail("  Person@Example.COM "))
        assertNull(emailValidationError("person@example.com"))
        assertEquals(AuthError.InvalidEmail, emailValidationError("person@example"))
    }

    @Test
    fun `strong password requires all factors`() {
        assertNull(strongPasswordValidationError("StrongPass1!"))
        assertEquals(AuthError.WeakPassword, strongPasswordValidationError("onlylowercase1!"))
        assertEquals(AuthError.WeakPassword, strongPasswordValidationError("NoSymbol12345"))
    }

    @Test
    fun `password confirmation must match`() {
        assertNull(passwordConfirmationError("StrongPass1!", "StrongPass1!"))
        assertEquals(AuthError.PasswordMismatch, passwordConfirmationError("StrongPass1!", "StrongPass2!"))
    }

    @Test
    fun `valid recovery link reads tokens from fragment`() {
        val result = parseRecoveryLink(
            "lifetrack://auth/recovery#access_token=access.jwt&refresh_token=refresh.jwt&type=recovery&expires_in=7200",
        )

        assertTrue(result is AuthResult.Success)
        val tokens = (result as AuthResult.Success).value
        assertEquals("access.jwt", tokens.accessToken)
        assertEquals("refresh.jwt", tokens.refreshToken)
        assertEquals(7_200, tokens.expiresInSeconds)
    }

    @Test
    fun `recovery link rejects unexpected destination or auth error`() {
        assertRecoveryFailure("lifetrack://other/recovery#access_token=a&refresh_token=r&type=recovery")
        assertRecoveryFailure("lifetrack://auth/recovery#error=access_denied&error_code=otp_expired&type=recovery")
        assertRecoveryFailure("lifetrack://auth/recovery#access_token=a&refresh_token=r")
    }

    private fun assertRecoveryFailure(link: String) {
        val result = parseRecoveryLink(link)
        assertTrue(result is AuthResult.Failure)
        assertEquals(AuthError.RecoveryLinkInvalid, (result as AuthResult.Failure).failure.error)
    }
}
