package com.yezdiconnect.app.obd

/** Live fuel use. [kmPerLitre] is null when the bike is (nearly) standing still. */
data class FuelRate(val litresPerHour: Double, val kmPerLitre: Double?)

object FuelEconomy {
    private const val STOICH_AFR = 14.7          // petrol air:fuel ratio by mass
    private const val PETROL_G_PER_L = 745.0     // density of petrol
    private const val AIR_MOLAR_MASS = 0.028_97  // kg/mol
    private const val GAS_CONSTANT = 8.314       // J/(mol·K)

    /** Yezdi Adventure: 334 cc single cylinder. */
    const val ADVENTURE_DISPLACEMENT_L = 0.334
    /** Typical volumetric efficiency for a small 4-stroke; tuned later against fill-ups. */
    const val DEFAULT_VE = 0.85

    /** Air flow (g/s) from a MAF sensor reading. */
    fun fromMaf(mafGps: Double, speedKmh: Double): FuelRate = rate(mafGps, speedKmh)

    /**
     * Air flow estimated with the speed-density method when the bike has no MAF sensor:
     * air mass per intake stroke from manifold pressure, intake air temperature and displacement;
     * a 4-stroke makes one intake stroke every 2 revolutions.
     */
    fun fromSpeedDensity(
        mapKpa: Double, intakeAirTempC: Double, rpm: Double, speedKmh: Double,
        displacementL: Double = ADVENTURE_DISPLACEMENT_L, volumetricEfficiency: Double = DEFAULT_VE,
    ): FuelRate {
        val tempK = intakeAirTempC + 273.15
        val airKgPerStroke = (mapKpa * 1000) * (displacementL / 1000) * volumetricEfficiency * AIR_MOLAR_MASS / (GAS_CONSTANT * tempK)
        val strokesPerSecond = rpm / 120.0
        return rate(airKgPerStroke * strokesPerSecond * 1000, speedKmh)
    }

    private fun rate(airGps: Double, speedKmh: Double): FuelRate {
        val litresPerHour = airGps / STOICH_AFR * 3600 / PETROL_G_PER_L
        val kmPerLitre = if (speedKmh >= MIN_MOVING_KMH && litresPerHour > 0) speedKmh / litresPerHour else null
        return FuelRate(litresPerHour, kmPerLitre)
    }

    const val MIN_MOVING_KMH = 5.0

    /**
     * Exact economy from the fill-up log, using the "full tank to full tank" method: the distance
     * between two full fills divided by all fuel added after the first one (including the second).
     * Partial fills in between are counted. Entries before the first full fill are ignored.
     */
    fun fromFillUps(fills: List<FillUpEntry>): FillUpEconomy? {
        val sorted = fills.sortedBy { it.odometerKm }
        val intervals = mutableListOf<Double>()
        var start: FillUpEntry? = null
        var litres = 0.0
        for (f in sorted) {
            if (start == null) {
                if (f.fullTank) start = f
                continue
            }
            litres += f.litres
            if (f.fullTank) {
                val km = f.odometerKm - start.odometerKm
                if (km > 0 && litres > 0) intervals += km / litres
                start = f
                litres = 0.0
            }
        }
        if (intervals.isEmpty()) return null
        // Overall = total distance / total fuel over all complete intervals.
        val firstFull = sorted.first { it.fullTank }
        val lastFull = sorted.last { it.fullTank }
        val totalKm = lastFull.odometerKm - firstFull.odometerKm
        val totalLitres = sorted.filter { it.odometerKm > firstFull.odometerKm && it.odometerKm <= lastFull.odometerKm }.sumOf { it.litres }
        return FillUpEconomy(average = totalKm / totalLitres, last = intervals.last())
    }
}

/** A fuel stop as the rider enters it. */
data class FillUpEntry(val odometerKm: Double, val litres: Double, val fullTank: Boolean)

data class FillUpEconomy(val average: Double, val last: Double)
