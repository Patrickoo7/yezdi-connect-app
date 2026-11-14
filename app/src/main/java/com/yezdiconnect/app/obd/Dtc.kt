package com.yezdiconnect.app.obd

/** A diagnostic trouble code such as "P0301", with a plain-English description when known. */
data class Dtc(val code: String, val pending: Boolean = false) {
    val description: String get() = DtcDescriptions.describe(code)
}

object DtcDecoder {

    /**
     * Decodes mode 03 (stored) or mode 07 (pending) replies. Each frame starts with 0x43 / 0x47.
     * CAN replies have a count byte after the mode byte (so an odd number of bytes follows);
     * K-line replies don't, and pad unused slots with 00 00.
     */
    fun decode(lines: List<String>, mode: Int): List<Dtc> {
        val reply = mode + 0x40
        val codes = mutableListOf<Dtc>()
        for (frame in ElmResponse.frames(lines)) {
            if (frame.firstOrNull() != reply) continue
            var rest = frame.drop(1)
            if (rest.size % 2 == 1) rest = rest.drop(1) // CAN count byte
            rest.chunked(2).forEach { (a, b) ->
                if (a != 0 || b != 0) codes += Dtc(format(a, b), pending = mode == 0x07)
            }
        }
        return codes.distinct()
    }

    fun format(a: Int, b: Int): String {
        val system = "PCBU"[(a shr 6) and 0x3]
        val first = (a shr 4) and 0x3
        return "%c%d%X%02X".format(system, first, a and 0xF, b)
    }
}

/** Generic SAE (P0xxx) descriptions most relevant to a single-cylinder fuel-injected bike. */
object DtcDescriptions {
    private val generic = mapOf(
        "P0105" to "Manifold pressure (MAP) sensor circuit",
        "P0106" to "Manifold pressure (MAP) sensor range/performance",
        "P0107" to "Manifold pressure (MAP) sensor low",
        "P0108" to "Manifold pressure (MAP) sensor high",
        "P0110" to "Intake air temperature sensor circuit",
        "P0112" to "Intake air temperature sensor low",
        "P0113" to "Intake air temperature sensor high",
        "P0115" to "Engine coolant/head temperature sensor circuit",
        "P0117" to "Engine temperature sensor low",
        "P0118" to "Engine temperature sensor high",
        "P0120" to "Throttle position sensor circuit",
        "P0122" to "Throttle position sensor low",
        "P0123" to "Throttle position sensor high",
        "P0130" to "Oxygen (O2) sensor circuit",
        "P0131" to "Oxygen (O2) sensor low voltage",
        "P0132" to "Oxygen (O2) sensor high voltage",
        "P0133" to "Oxygen (O2) sensor slow response",
        "P0134" to "Oxygen (O2) sensor no activity",
        "P0135" to "Oxygen (O2) sensor heater circuit",
        "P0171" to "Fuel mixture too lean",
        "P0172" to "Fuel mixture too rich",
        "P0201" to "Fuel injector circuit (cylinder 1)",
        "P0230" to "Fuel pump circuit",
        "P0300" to "Random misfire detected",
        "P0301" to "Misfire detected (cylinder 1)",
        "P0335" to "Crankshaft position sensor circuit",
        "P0340" to "Camshaft position sensor circuit",
        "P0351" to "Ignition coil circuit",
        "P0500" to "Vehicle speed sensor",
        "P0505" to "Idle control system",
        "P0560" to "System (battery) voltage",
        "P0562" to "System (battery) voltage low",
        "P0563" to "System (battery) voltage high",
        "P0601" to "ECU memory checksum error",
        "P0650" to "Check-engine lamp circuit",
    )

    fun describe(code: String): String = generic[code.uppercase()] ?: when {
        code.length == 5 && code[1] == '1' -> "Manufacturer-specific code (ask your service centre)"
        code.startsWith("P") -> "Powertrain code (no description in the app yet)"
        code.startsWith("C") -> "Chassis code (e.g. ABS)"
        code.startsWith("B") -> "Body code"
        code.startsWith("U") -> "Network/communication code"
        else -> "Unknown code"
    }
}
