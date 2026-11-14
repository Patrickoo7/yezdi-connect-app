package com.yezdiconnect.app.obd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FuelEconomyTest {

    @Test fun `maf based rate`() {
        // 1.0 g/s air → 0.068 g/s fuel → 0.3287 L/h; at 30 km/h ≈ 91 km/l
        val r = FuelEconomy.fromMaf(1.0, 30.0)
        assertEquals(0.3287, r.litresPerHour, 0.001)
        assertEquals(91.3, r.kmPerLitre!!, 0.2)
    }

    @Test fun `speed density gives a plausible cruise figure for a 334 cc single`() {
        // 60 km/h cruise: ~4000 rpm, 45 kPa manifold pressure, 30 °C intake air
        val r = FuelEconomy.fromSpeedDensity(mapKpa = 45.0, intakeAirTempC = 30.0, rpm = 4000.0, speedKmh = 60.0)
        // A 334 cc single typically does 25–45 km/l at steady cruise.
        assertEquals(35.0, r.kmPerLitre!!, 12.0)
    }

    @Test fun `standing still has no km per litre`() {
        assertNull(FuelEconomy.fromSpeedDensity(30.0, 30.0, 1400.0, 0.0).kmPerLitre)
    }

    @Test fun `full tank to full tank`() {
        val fills = listOf(
            FillUpEntry(odometerKm = 1000.0, litres = 8.0, fullTank = true),
            FillUpEntry(odometerKm = 1150.0, litres = 3.0, fullTank = false),
            FillUpEntry(odometerKm = 1300.0, litres = 6.0, fullTank = true), // 300 km / 9 L = 33.3
            FillUpEntry(odometerKm = 1660.0, litres = 10.0, fullTank = true), // 360 km / 10 L = 36
        )
        val e = FuelEconomy.fromFillUps(fills)!!
        assertEquals(36.0, e.last, 0.01)
        assertEquals(660.0 / 19.0, e.average, 0.01)
    }

    @Test fun `needs two full fills`() {
        assertNull(FuelEconomy.fromFillUps(listOf(FillUpEntry(1000.0, 8.0, true), FillUpEntry(1200.0, 5.0, false))))
        assertNull(FuelEconomy.fromFillUps(emptyList()))
    }
}
