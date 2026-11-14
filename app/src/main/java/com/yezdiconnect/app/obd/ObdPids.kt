package com.yezdiconnect.app.obd

/** Standard OBD-II mode 01 values the app reads, with their SAE J1979 formulas. */
enum class ObdPid(val pid: Int, val label: String, val unit: String, val decode: (List<Int>) -> Double?) {
    ENGINE_LOAD(0x04, "Engine load", "%", { b -> b.a?.let { it * 100.0 / 255 } }),
    COOLANT_TEMP(0x05, "Engine temp", "°C", { b -> b.a?.let { it - 40.0 } }),
    INTAKE_PRESSURE(0x0B, "Intake pressure", "kPa", { b -> b.a?.toDouble() }),
    RPM(0x0C, "RPM", "rpm", { b -> b.ab?.let { it / 4.0 } }),
    SPEED(0x0D, "Speed", "km/h", { b -> b.a?.toDouble() }),
    INTAKE_AIR_TEMP(0x0F, "Intake air temp", "°C", { b -> b.a?.let { it - 40.0 } }),
    MAF(0x10, "Air flow", "g/s", { b -> b.ab?.let { it / 100.0 } }),
    THROTTLE(0x11, "Throttle", "%", { b -> b.a?.let { it * 100.0 / 255 } }),
    FUEL_LEVEL(0x2F, "Fuel level", "%", { b -> b.a?.let { it * 100.0 / 255 } }),
    MODULE_VOLTAGE(0x42, "ECU voltage", "V", { b -> b.ab?.let { it / 1000.0 } }),
    OIL_TEMP(0x5C, "Oil temp", "°C", { b -> b.a?.let { it - 40.0 } }),
    ;

    companion object {
        fun of(pid: Int): ObdPid? = entries.firstOrNull { it.pid == pid }
    }
}

private val List<Int>.a: Int? get() = getOrNull(0)
private val List<Int>.ab: Int? get() = if (size >= 2) this[0] * 256 + this[1] else null

object ObdDecoders {

    /**
     * Decodes a "supported PIDs" bitmap reply (mode 01 PID 00/20/40/…): 4 bytes, MSB first,
     * bit n set ⇒ PID base+n+1 is supported.
     */
    fun supportedPids(base: Int, data: List<Int>): Set<Int> {
        val out = mutableSetOf<Int>()
        data.take(4).forEachIndexed { byteIndex, byte ->
            for (bit in 0 until 8) {
                if (byte and (0x80 shr bit) != 0) out += base + byteIndex * 8 + bit + 1
            }
        }
        return out
    }

    /** "12.6V" (reply to ATRV) → 12.6 */
    fun adapterVoltage(lines: List<String>): Double? =
        lines.firstNotNullOfOrNull { Regex("""(\d+(?:\.\d+)?)\s*V""").find(it)?.groupValues?.get(1)?.toDoubleOrNull() }

    /**
     * Vehicle identification number from a mode 09 PID 02 reply. Handles CAN (one multi-frame
     * reply with a count byte) and K-line (several "49 02 nn a b c d" lines).
     */
    fun vin(lines: List<String>): String? {
        val frames = ElmResponse.frames(lines).filter { it.size >= 2 && it[0] == 0x49 && it[1] == 0x02 }
        if (frames.isEmpty()) return null
        val bytes = if (frames.size == 1) {
            frames[0].drop(3) // 49 02 <count>
        } else {
            frames.sortedBy { it.getOrNull(2) ?: 0 }.flatMap { it.drop(3) } // 49 02 <seq> + 4 bytes
        }
        val text = bytes.filter { it in 0x30..0x5A }.map { it.toChar() }.joinToString("")
        return text.takeLast(17).takeIf { it.length == 17 }
    }
}
