package com.yezdiconnect.app.rides

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "rides")
data class Ride(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val endedAt: Long? = null,
    val distanceMeters: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
) {
    val durationMillis: Long? get() = endedAt?.let { it - startedAt }
    val avgSpeedKmh: Double?
        get() = durationMillis?.takeIf { it > 0 }?.let { distanceMeters / 1000.0 / (it / 3_600_000.0) }
}

@Entity(
    tableName = "ride_points",
    foreignKeys = [ForeignKey(entity = Ride::class, parentColumns = ["id"], childColumns = ["rideId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("rideId")],
)
data class RidePoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rideId: Long,
    val time: Long,
    val lat: Double,
    val lng: Double,
    val speedKmh: Double,
    val accuracyMeters: Float,
)
