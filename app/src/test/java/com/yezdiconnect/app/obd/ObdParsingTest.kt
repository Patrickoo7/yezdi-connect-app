package com.yezdiconnect.app.obd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ObdParsingTest {

    private fun lines(raw: String) = ElmResponse.lines(raw)

    @Test fun `single frame with and without spaces`() {
        assertEquals(listOf(0x1A, 0xF8), ElmResponse.payload(lines("41 0C 1A F8\r\r>"), 0x01, 0x0C))
        assertEquals(listOf(0x1A, 0xF8), ElmResponse.payload(lines("410C1AF8\r>"), 0x01, 0x0C))
    }

    @Test fun `searching line and prompt are ignored`() {
        assertEquals(listOf(0x3C), ElmResponse.payload(lines("SEARCHING...\r41 0D 3C\r\r>"), 0x01, 0x0D))
    }

    @Test fun `errors are recognised`() {
        assertEquals("NO DATA", ElmResponse.error(lines("NO DATA\r>")))
        assertEquals("UNABLE TO CONNECT", ElmResponse.error(lines("SEARCHING...\rUNABLE TO CONNECT\r>")))
        assertNull(ElmResponse.error(lines("BUS INIT: ...OK\r41 00 BE 1F A8 13\r>")))
    }

    @Test fun `pid formulas`() {
        assertEquals(1726.0, ObdPid.RPM.decode(listOf(0x1A, 0xF8))!!, 0.001)
        assertEquals(60.0, ObdPid.SPEED.decode(listOf(0x3C))!!, 0.001)
        assertEquals(83.0, ObdPid.COOLANT_TEMP.decode(listOf(0x7B))!!, 0.001)
        assertEquals(20.0, ObdPid.THROTTLE.decode(listOf(0x33))!!, 0.001)
        assertEquals(13.5, ObdPid.MODULE_VOLTAGE.decode(listOf(0x34, 0xBC))!!, 0.001)
        assertNull(ObdPid.RPM.decode(listOf(0x1A)))
    }

    @Test fun `supported pid bitmap`() {
        val pids = ObdDecoders.supportedPids(0x00, listOf(0xBE, 0x1F, 0xA8, 0x13))
        assertEquals(setOf(0x01, 0x03, 0x04, 0x05, 0x06, 0x07, 0x0C, 0x0D, 0x0E, 0x0F, 0x10, 0x11, 0x13, 0x15, 0x1C, 0x1F, 0x20), pids)
    }

    @Test fun `fault codes from CAN reply with count byte`() {
        val codes = DtcDecoder.decode(lines("43 02 01 33 03 01\r>"), 0x03)
        assertEquals(listOf("P0133", "P0301"), codes.map { it.code })
        assertEquals("Misfire detected (cylinder 1)", codes[1].description)
    }

    @Test fun `fault codes from K-line reply padded with zeros`() {
        val codes = DtcDecoder.decode(lines("43 01 13 C1 23 00 00\r>"), 0x03)
        assertEquals(listOf("P0113", "U0123"), codes.map { it.code })
    }

    @Test fun `no fault codes`() {
        assertTrue(DtcDecoder.decode(lines("43 00\r>"), 0x03).isEmpty())
        assertTrue(DtcDecoder.decode(lines("43 00 00 00 00 00 00\r>"), 0x03).isEmpty())
    }

    @Test fun `code letters and manufacturer codes`() {
        assertEquals("C0035", DtcDecoder.format(0x40, 0x35))
        assertEquals("B1234", DtcDecoder.format(0x92, 0x34))
        assertTrue(DtcDescriptions.describe("P1234").startsWith("Manufacturer-specific"))
    }

    @Test fun `vin from CAN multi-frame`() {
        val vin = ObdDecoders.vin(lines("014\r0: 49 02 01 4D 44 32\r1: 41 42 43 44 45 46 47\r2: 31 32 33 34 35 36 37\r>"))
        assertEquals("MD2ABCDEFG1234567", vin)
    }

    @Test fun `vin from K-line lines`() {
        val raw = "49 02 01 00 00 00 4D\r49 02 02 44 32 41 42\r49 02 03 43 44 45 46\r49 02 04 47 31 32 33\r49 02 05 34 35 36 37\r>"
        assertEquals("MD2ABCDEFG1234567", ObdDecoders.vin(lines(raw)))
    }

    @Test fun `adapter voltage`() {
        assertEquals(12.6, ObdDecoders.adapterVoltage(lines("12.6V\r>"))!!, 0.001)
    }
}
