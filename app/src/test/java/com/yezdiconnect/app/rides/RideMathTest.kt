package com.yezdiconnect.app.rides

import org.junit.Assert.assertEquals
import org.junit.Test

class RideMathTest {

    @Test fun `haversine matches a known distance`() {
        // Bengaluru (MG Road) to Mysuru Palace: about 128 km in a straight line.
        val d = RideMath.haversineMeters(12.9756, 77.6066, 12.3052, 76.6552)
        assertEquals(127_000.0, d, 3_000.0)
    }

    @Test fun `zero distance for the same point`() {
        assertEquals(0.0, RideMath.haversineMeters(12.0, 77.0, 12.0, 77.0), 1e-9)
    }

    @Test fun `normal riding step is counted`() {
        // ~11 m north in 1 s ≈ 40 km/h
        val step = RideMath.usableStep(12.0, 77.0, 0, 12.0001, 77.0, 1000, 5f)
        assertEquals(11.1, step, 0.2)
    }

    @Test fun `inaccurate fix is ignored`() {
        assertEquals(0.0, RideMath.usableStep(12.0, 77.0, 0, 12.0001, 77.0, 1000, 80f), 0.0)
    }

    @Test fun `standing-still jitter is ignored`() {
        // ~1 m
        assertEquals(0.0, RideMath.usableStep(12.0, 77.0, 0, 12.00001, 77.0, 1000, 5f), 0.0)
    }

    @Test fun `impossible jump is ignored`() {
        // ~1.1 km in 1 s
        assertEquals(0.0, RideMath.usableStep(12.0, 77.0, 0, 12.01, 77.0, 1000, 5f), 0.0)
    }
}
