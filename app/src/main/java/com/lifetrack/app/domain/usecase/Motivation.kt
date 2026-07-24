package com.lifetrack.app.domain.usecase

import com.lifetrack.app.domain.model.WaterEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

fun hydrationStreak(
    entries: List<WaterEntry>,
    goalMl: Int,
    date: LocalDate = LocalDate.now(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): Int {
    val totals = entries.groupBy { entry ->
        Instant.ofEpochMilli(entry.loggedAt).atZone(zoneId).toLocalDate()
    }.mapValues { (_, dayEntries) -> dayEntries.sumOf { it.amountMl } }
    var streak = 0
    var cursor = if ((totals[date] ?: 0) >= goalMl) date else date.minusDays(1)
    while ((totals[cursor] ?: 0) >= goalMl) {
        streak++
        cursor = cursor.minusDays(1)
    }
    return streak
}
