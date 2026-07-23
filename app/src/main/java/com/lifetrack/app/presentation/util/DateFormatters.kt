package com.lifetrack.app.presentation.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

fun formatDateTime(timestamp: Long): String = Instant.ofEpochMilli(timestamp)
    .atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(Locale.getDefault()))

fun formatDuration(minutes: Long): String = "${minutes / 60} h ${minutes % 60} min"

fun defaultBedtimeInput(): String = LocalDateTime.now().minusHours(8)
    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))

fun defaultWakeInput(): String = LocalDateTime.now()
    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))

fun parseDateTime(value: String): Long? = runCatching {
    LocalDateTime.parse(value.trim(), DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}.getOrNull()
