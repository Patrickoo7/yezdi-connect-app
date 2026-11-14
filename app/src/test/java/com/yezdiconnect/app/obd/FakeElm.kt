package com.yezdiconnect.app.obd

/**
 * Scripted ELM327: answers each command from [replies] (CR line endings and the '>' prompt added
 * like a real adapter). Unknown commands get "?". Records what was sent.
 */
class FakeElm(private val replies: Map<String, String>) : ElmTransport {
    val sent = mutableListOf<String>()
    var closed = false

    override suspend fun request(command: String, timeoutMs: Long): String {
        sent += command
        val reply = replies[command] ?: "?"
        return reply.replace("\n", "\r") + "\r\r>"
    }

    override fun close() { closed = true }

    companion object {
        /** A typical CAN bike: RPM, speed, coolant, MAP, IAT, throttle, load supported; no MAF. */
        val canBike = mapOf(
            "ATZ" to "\nELM327 v1.5",
            "ATE0" to "OK", "ATL0" to "OK", "ATS1" to "OK", "ATH0" to "OK", "ATAT1" to "OK", "ATSP0" to "OK",
            // PIDs 04 05 0B 0C 0D 0F 11 supported; 0x20 not set.
            "0100" to "SEARCHING...\n41 00 18 3A 80 00",
            "ATDPN" to "A6",
            "010C" to "41 0C 1A F8",   // 1726 rpm
            "010D" to "41 0D 3C",      // 60 km/h
            "0105" to "41 05 7B",      // 83 °C
            "010B" to "41 0B 2D",      // 45 kPa
            "010F" to "41 0F 46",      // 30 °C
            "0111" to "41 11 33",      // 20 %
            "0104" to "41 04 66",      // 40 %
            "0110" to "NO DATA",
            "03" to "43 02 01 33 03 01",
            "07" to "47 00",
            "0902" to "014\n0: 49 02 01 4D 44 32\n1: 41 42 43 44 45 46 47\n2: 31 32 33 34 35 36 37",
            "ATRV" to "12.6V",
        )
    }
}
