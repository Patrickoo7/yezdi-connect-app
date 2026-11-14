package com.yezdiconnect.app.obd

/** A link to an ELM327-compatible adapter (Bluetooth LE, classic Bluetooth, or a test fake). */
interface ElmTransport {
    /** Sends [command] (without "\r") and returns everything the adapter replies before its '>' prompt. */
    suspend fun request(command: String, timeoutMs: Long): String
    fun close()
}

class ObdException(message: String) : Exception(message)

/** What the adapter and the bike's engine computer (ECU) told us when connecting. */
data class ObdInfo(val adapter: String, val protocol: String, val supportedPids: Set<Int>)

/**
 * Talks OBD-II through an ELM327-compatible adapter. Read-only: it never sends commands that
 * clear codes or change anything on the bike.
 */
class ElmSession(private val io: ElmTransport) {

    private suspend fun cmd(command: String, timeoutMs: Long = 2_000): List<String> =
        ElmResponse.lines(io.request(command, timeoutMs))

    /** Resets the adapter, lets it find the bike's protocol, and asks which values the ECU supports. */
    suspend fun initialize(): ObdInfo {
        val id = cmd("ATZ", 3_000).lastOrNull { it.startsWith("ELM") || it.startsWith("STN") || it.startsWith("OBDLINK") }
            ?: "ELM327-compatible"
        for (c in listOf("ATE0", "ATL0", "ATS1", "ATH0", "ATAT1", "ATSP0")) cmd(c)

        // The first request makes the adapter search through the protocols; that can take a while.
        val first = cmd("0100", 15_000)
        ElmResponse.error(first)?.let { throw ObdException(explain(it)) }
        val supported = mutableSetOf<Int>()
        var base = 0x00
        var reply: List<String>? = first
        while (reply != null) {
            val data = ElmResponse.payload(reply, 0x01, base) ?: break
            val pids = ObdDecoders.supportedPids(base, data)
            supported += pids
            base += 0x20
            reply = if (base in pids && base <= 0xA0) cmd("01%02X".format(base)) else null
        }
        if (supported.isEmpty()) throw ObdException("The bike answered, but in a format the app doesn't understand yet.")

        val protocol = cmd("ATDPN").firstOrNull()?.removePrefix("A") ?: "?"
        return ObdInfo(adapter = id, protocol = protocolName(protocol), supportedPids = supported - listOf(0x20, 0x40, 0x60, 0x80, 0xA0, 0xC0))
    }

    /** One mode 01 value, or null if the ECU didn't answer it. */
    suspend fun read(pid: ObdPid): Double? {
        val lines = cmd("01%02X".format(pid.pid))
        if (ElmResponse.error(lines) != null) return null
        return ElmResponse.payload(lines, 0x01, pid.pid)?.let(pid.decode)
    }

    /** Stored (mode 03) and pending (mode 07) fault codes. */
    suspend fun readFaultCodes(): List<Dtc> {
        val stored = cmd("03", 5_000).let { if (ElmResponse.error(it) == "NO DATA") emptyList() else DtcDecoder.decode(it, 0x03) }
        val pending = cmd("07", 5_000).let { if (ElmResponse.error(it) == "NO DATA") emptyList() else DtcDecoder.decode(it, 0x07) }
        return stored + pending.filter { p -> stored.none { it.code == p.code } }
    }

    suspend fun readVin(): String? = cmd("0902", 5_000).let(ObdDecoders::vin)

    /** Battery voltage measured by the adapter at the OBD port. */
    suspend fun batteryVoltage(): Double? = ObdDecoders.adapterVoltage(cmd("ATRV"))

    fun close() = io.close()

    companion object {
        fun explain(error: String): String = when (error) {
            "UNABLE TO CONNECT", "NO DATA" -> "The adapter found no engine computer. Is the ignition on and the adapter firmly plugged in?"
            "BUS INIT", "BUS ERROR", "CAN ERROR" -> "The adapter couldn't talk to the bike (bus error). Check the cable/adapter fit."
            "STOPPED" -> "The adapter stopped the request. Try again."
            else -> "The adapter replied \"$error\"."
        }

        fun protocolName(number: String): String = when (number.trim()) {
            "1" -> "SAE J1850 PWM"
            "2" -> "SAE J1850 VPW"
            "3" -> "ISO 9141-2 (K-line)"
            "4" -> "ISO 14230 KWP2000 (K-line, slow init)"
            "5" -> "ISO 14230 KWP2000 (K-line, fast init)"
            "6" -> "CAN 11-bit 500 kbit/s"
            "7" -> "CAN 29-bit 500 kbit/s"
            "8" -> "CAN 11-bit 250 kbit/s"
            "9" -> "CAN 29-bit 250 kbit/s"
            else -> "Protocol $number"
        }
    }
}
