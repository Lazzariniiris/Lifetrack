package com.lifetrack.app.presentation.viewmodel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MealValidationTest {
    @Test fun `nutrition accepts non negative finite values`() {
        assertTrue(validNutrition(500.0, 25.0, 60.0, 18.0))
    }
    @Test fun `nutrition rejects negative and non finite values`() {
        assertFalse(validNutrition(-1.0, 25.0, 60.0, 18.0))
        assertFalse(validNutrition(Double.NaN, 25.0, 60.0, 18.0))
    }
}
