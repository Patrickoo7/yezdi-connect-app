package com.yezdiconnect.app.obd

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ElmSessionTest {

    @Test fun `initialize finds protocol and supported values`() = runBlocking {
        val fake = FakeElm(FakeElm.canBike)
        val info = ElmSession(fake).initialize()
        assertEquals("ELM327 V1.5", info.adapter)
        assertEquals("CAN 11-bit 500 kbit/s", info.protocol)
        assertEquals(setOf(0x04, 0x05, 0x0B, 0x0C, 0x0D, 0x0F, 0x11), info.supportedPids)
        assertEquals("ATZ", fake.sent.first())
        assertTrue("never clears codes", fake.sent.none { it == "04" })
    }

    @Test fun `follows the supported-pid chain`() = runBlocking {
        val replies = FakeElm.canBike + mapOf(
            "0100" to "41 00 00 00 00 01", // only 0x20 → ask 0120
            "0120" to "41 20 00 00 40 00", // 0x32... bit for 0x32? byte2 bit1 → 0x20+16+2 = 0x32
        )
        val info = ElmSession(FakeElm(replies)).initialize()
        assertEquals(setOf(0x32), info.supportedPids)
    }

    @Test fun `ignition off gives a clear error`() = runBlocking {
        val replies = FakeElm.canBike + ("0100" to "SEARCHING...\nUNABLE TO CONNECT")
        try {
            ElmSession(FakeElm(replies)).initialize()
            fail("expected ObdException")
        } catch (e: ObdException) {
            assertTrue(e.message!!.contains("ignition"))
        }
    }

    @Test fun `reads values, codes, vin and voltage`() = runBlocking {
        val s = ElmSession(FakeElm(FakeElm.canBike))
        s.initialize()
        assertEquals(1726.0, s.read(ObdPid.RPM)!!, 0.01)
        assertEquals(60.0, s.read(ObdPid.SPEED)!!, 0.01)
        assertNull(s.read(ObdPid.MAF)) // NO DATA
        assertEquals(listOf("P0133", "P0301"), s.readFaultCodes().map { it.code })
        assertEquals("MD2ABCDEFG1234567", s.readVin())
        assertEquals(12.6, s.batteryVoltage()!!, 0.001)
    }
}
