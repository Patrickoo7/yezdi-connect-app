package com.yezdiconnect.app.nav

enum class ManeuverType {
    STRAIGHT, TURN_LEFT, TURN_RIGHT, SLIGHT_LEFT, SLIGHT_RIGHT, SHARP_LEFT, SHARP_RIGHT,
    KEEP_LEFT, KEEP_RIGHT, U_TURN, ROUNDABOUT, MERGE, DESTINATION, UNKNOWN,
}

enum class DistanceUnit(val label: String) { METERS("m"), KILOMETERS("km"), FEET("ft"), MILES("mi"), YARDS("yd") }

data class Distance(val value: Double, val unit: DistanceUnit) {
    val meters: Double
        get() = when (unit) {
            DistanceUnit.METERS -> value
            DistanceUnit.KILOMETERS -> value * 1000
            DistanceUnit.FEET -> value * 0.3048
            DistanceUnit.MILES -> value * 1609.344
            DistanceUnit.YARDS -> value * 0.9144
        }

    fun display(): String {
        val v = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
        return "$v ${unit.label}"
    }
}

/** One navigation step as shown by Google Maps' ongoing navigation notification. */
data class Maneuver(
    val type: ManeuverType,
    val distance: Distance?,
    val road: String?,
    /** Roundabout exit number, when Google Maps says e.g. "take the 2nd exit". */
    val roundaboutExit: Int? = null,
    /** Raw instruction text, kept for debugging. */
    val instruction: String,
)
