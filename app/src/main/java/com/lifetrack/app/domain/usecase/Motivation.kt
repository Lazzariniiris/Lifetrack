package com.lifetrack.app.domain.usecase

import com.lifetrack.app.domain.model.WaterEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val dailyMessages = listOf(
    "Un pequeno registro hoy tambien construye bienestar.",
    "La constancia se forma con acciones simples y repetidas.",
    "Tu progreso no necesita ser perfecto para ser valioso.",
    "Cuidarte tambien es reservar un momento para vos.",
    "Cada meta cumplida empieza con una accion posible.",
    "Observa tu ritmo, celebra tus avances y continua a tu manera.",
    "Un dia equilibrado se construye paso a paso.",
)

fun dailyMotivation(date: LocalDate = LocalDate.now()): String =
    dailyMessages[(date.toEpochDay() % dailyMessages.size).toInt()]

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
