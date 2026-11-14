package com.yezdiconnect.app.nav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ManeuverParserTest {

    @Test fun `distance in title, turn in text`() {
        val m = ManeuverParser.parse("350 m", "Turn left onto MG Road", null, "12 min · 4.5 km · 10:42 ETA")!!
        assertEquals(ManeuverType.TURN_LEFT, m.type)
        assertEquals(350.0, m.distance!!.meters, 0.001)
        assertEquals("MG Road", m.road)
    }

    @Test fun `trip summary distance is not taken as step distance`() {
        val m = ManeuverParser.parse(null, "Turn right", null, "12 min · 4.5 km · 10:42 ETA")!!
        assertEquals(ManeuverType.TURN_RIGHT, m.type)
        assertNull(m.distance)
    }

    @Test fun `kilometres with decimal comma`() {
        assertEquals(1200.0, ManeuverParser.parseDistance("1,2 km")!!.meters, 0.001)
        assertEquals(1500.0, ManeuverParser.parseDistance("1.5 km")!!.meters, 0.001)
    }

    @Test fun `imperial units`() {
        assertEquals(500 * 0.3048, ManeuverParser.parseDistance("500 ft")!!.meters, 0.001)
        assertEquals(0.3 * 1609.344, ManeuverParser.parseDistance("0.3 mi")!!.meters, 0.001)
    }

    @Test fun `specific turns win over general ones`() {
        assertEquals(ManeuverType.SLIGHT_LEFT, ManeuverParser.parse("100 m", "Slight left onto NH 48")!!.type)
        assertEquals(ManeuverType.SHARP_RIGHT, ManeuverParser.parse("50 m", "Sharp right")!!.type)
        assertEquals(ManeuverType.KEEP_RIGHT, ManeuverParser.parse("1 km", "Keep right to stay on Outer Ring Road")!!.type)
        assertEquals(ManeuverType.U_TURN, ManeuverParser.parse("200 m", "Make a U-turn")!!.type)
    }

    @Test fun `roundabout exit as ordinal number and as word`() {
        val a = ManeuverParser.parse("150 m", "At the roundabout, take the 2nd exit onto Hosur Road")!!
        assertEquals(ManeuverType.ROUNDABOUT, a.type)
        assertEquals(2, a.roundaboutExit)
        assertEquals("Hosur Road", a.road)
        assertEquals(3, ManeuverParser.parse("At the roundabout, take the third exit")!!.roundaboutExit)
    }

    @Test fun `destination`() {
        assertEquals(ManeuverType.DESTINATION, ManeuverParser.parse("50 m", "Your destination is on the left")!!.type)
    }

    @Test fun `continue towards`() {
        val m = ManeuverParser.parse("2 km", "Continue towards Mysuru")!!
        assertEquals(ManeuverType.STRAIGHT, m.type)
        assertEquals("Mysuru", m.road)
    }

    @Test fun `distance only gives unknown type`() {
        val m = ManeuverParser.parse("400 m", "MG Road")!!
        assertEquals(ManeuverType.UNKNOWN, m.type)
        assertEquals(400.0, m.distance!!.meters, 0.001)
    }

    @Test fun `unrelated text gives null`() {
        assertNull(ManeuverParser.parse("Google Maps", "Navigation is running"))
        assertNull(ManeuverParser.parse(null, null))
    }

    @Test fun `numbers inside words are not distances`() {
        assertNull(ManeuverParser.parseDistance("NH48m"))
        assertNull(ManeuverParser.parseDistance("Sector 5"))
    }
}
