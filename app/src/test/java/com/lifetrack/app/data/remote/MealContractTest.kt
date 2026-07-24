package com.lifetrack.app.data.remote

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test

class MealContractTest {
    @Test fun `pending cloud payload contains durable defaults`() {
        val json = Json { encodeDefaults = true; explicitNulls = true }
        val payload = json.encodeToString(PendingMealCloudRow(id = "meal-id", userId = "user-id", photoPath = "user-id/meal-id.jpg"))
        assertTrue(payload.contains("\"status\":\"pending\""))
        assertTrue(payload.contains("\"foods_json\":[]"))
        assertTrue(payload.contains("\"calories\":null"))
    }
}
