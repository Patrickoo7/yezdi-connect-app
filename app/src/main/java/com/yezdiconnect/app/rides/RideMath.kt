package com.yezdiconnect.app.rides

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object RideMath {
    private const val EARTH_RADIUS_M = 6_371_008.8

    /** Great-circle distance in meters. */
    fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return 2 * EARTH_RADIUS_M * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    /**
     * Distance to add for a new GPS fix, or 0 when the fix should be ignored:
     * poor accuracy, GPS jitter while standing still, or an impossible jump.
     */
    fun usableStep(
        prevLat: Double, prevLng: Double, prevTime: Long,
        lat: Double, lng: Double, time: Long, accuracyMeters: Float,
    ): Double {
        if (accuracyMeters > MAX_ACCURACY_M) return 0.0
        val meters = haversineMeters(prevLat, prevLng, lat, lng)
        if (meters < MIN_STEP_M) return 0.0
        val seconds = (time - prevTime) / 1000.0
        if (seconds <= 0) return 0.0
        val kmh = meters / seconds * 3.6
        return if (kmh > MAX_PLAUSIBLE_KMH) 0.0 else meters
    }

    const val MAX_ACCURACY_M = 30f
    const val MIN_STEP_M = 3.0
    const val MAX_PLAUSIBLE_KMH = 250.0
}
