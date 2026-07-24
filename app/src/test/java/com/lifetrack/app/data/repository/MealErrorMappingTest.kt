package com.lifetrack.app.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MealErrorMappingTest {
    @Test fun `network class http errors are retryable`() {
        assertTrue(mealHttpError(429).retryable)
        assertTrue(mealHttpError(503).retryable)
    }

    @Test fun `invalid image and auth errors are terminal`() {
        assertFalse(mealHttpError(401).retryable)
        assertFalse(mealHttpError(413).retryable)
        assertFalse(mealHttpError(415).retryable)
        assertFalse(mealHttpError(422).retryable)
    }
}
