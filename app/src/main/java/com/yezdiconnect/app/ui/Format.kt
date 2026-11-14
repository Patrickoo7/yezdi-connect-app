package com.yezdiconnect.app.ui

import java.text.DateFormat
import java.util.Date
import java.util.Locale

fun formatKm(meters: Double): String = String.format(Locale.getDefault(), "%.1f km", meters / 1000.0)
fun formatKmh(kmh: Double): String = String.format(Locale.getDefault(), "%.0f km/h", kmh)

fun formatDuration(millis: Long): String {
    val totalMin = millis / 60_000
    val h = totalMin / 60
    val m = totalMin % 60
    return if (h > 0) "${h}h ${m}m" else "${m} min"
}

fun formatDateTime(epochMillis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(epochMillis))
