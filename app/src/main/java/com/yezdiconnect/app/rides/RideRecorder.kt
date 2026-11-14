package com.yezdiconnect.app.rides

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Live state of the ride being recorded, for the map screen. */
data class LiveRide(
    val rideId: Long,
    val startedAt: Long,
    val distanceMeters: Double,
    val speedKmh: Double,
    val maxSpeedKmh: Double,
    val path: List<Pair<Double, Double>>,
)

/** Accumulates GPS fixes into a ride and saves them. Used by [RideTrackingService]. */
class RideRecorder(private val dao: RideDao) {
    private val _live = MutableStateFlow<LiveRide?>(null)
    val live: StateFlow<LiveRide?> = _live.asStateFlow()

    private var lastAccepted: RidePoint? = null

    suspend fun start(now: Long) {
        if (_live.value != null) return
        // Close rides a crash left open, so they show up in history.
        dao.unfinished().forEach { dao.update(it.copy(endedAt = it.endedAt ?: now)) }
        val id = dao.insert(Ride(startedAt = now))
        lastAccepted = null
        _live.value = LiveRide(id, now, 0.0, 0.0, 0.0, emptyList())
    }

    suspend fun onLocation(time: Long, lat: Double, lng: Double, speedKmh: Double, accuracyMeters: Float) {
        val live = _live.value ?: return
        val prev = lastAccepted
        val step = if (prev == null) 0.0 else RideMath.usableStep(prev.lat, prev.lng, prev.time, lat, lng, time, accuracyMeters)
        val accept = prev == null && accuracyMeters <= RideMath.MAX_ACCURACY_M || step > 0.0

        _live.value = live.copy(
            speedKmh = speedKmh,
            distanceMeters = live.distanceMeters + step,
            maxSpeedKmh = if (accept) maxOf(live.maxSpeedKmh, speedKmh) else live.maxSpeedKmh,
            path = if (accept) live.path + (lat to lng) else live.path,
        )
        if (accept) {
            val point = RidePoint(rideId = live.rideId, time = time, lat = lat, lng = lng, speedKmh = speedKmh, accuracyMeters = accuracyMeters)
            // Update before the (suspending) insert, so a fix arriving meanwhile measures from this one.
            lastAccepted = point
            dao.insertPoint(point)
        }
    }

    suspend fun stop(now: Long) {
        val live = _live.value ?: return
        _live.value = null
        lastAccepted = null
        if (live.distanceMeters < MIN_RIDE_METERS) {
            dao.delete(live.rideId) // too short to be a ride
        } else {
            dao.update(Ride(live.rideId, live.startedAt, now, live.distanceMeters, live.maxSpeedKmh))
        }
    }

    companion object { const val MIN_RIDE_METERS = 100.0 }
}
