package com.yezdiconnect.app.obd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ObdManagerTest {
    @Test fun `uses MAF when the bike has it`() {
        val r = ObdManager.estimateFuel(mapOf(ObdPid.MAF to 1.0, ObdPid.INTAKE_PRESSURE to 45.0), 30.0)!!
        assertEquals(FuelEconomy.fromMaf(1.0, 30.0), r)
    }

    @Test fun `falls back to speed-density`() {
        val values = mapOf(ObdPid.INTAKE_PRESSURE to 45.0, ObdPid.INTAKE_AIR_TEMP to 30.0, ObdPid.RPM to 4000.0)
        assertEquals(FuelEconomy.fromSpeedDensity(45.0, 30.0, 4000.0, 60.0), ObdManager.estimateFuel(values, 60.0))
    }

    @Test fun `no estimate without enough values`() {
        assertNull(ObdManager.estimateFuel(mapOf(ObdPid.RPM to 4000.0), 60.0))
    }

    @Test fun `trip economy needs some distance and fuel`() {
        assertNull(ObdLive(emptyMap(), null, null, tripKm = 0.1, tripLitres = 0.001).tripKmPerLitre)
        assertEquals(35.0, ObdLive(emptyMap(), null, null, tripKm = 3.5, tripLitres = 0.1).tripKmPerLitre!!, 0.001)
    }
}
