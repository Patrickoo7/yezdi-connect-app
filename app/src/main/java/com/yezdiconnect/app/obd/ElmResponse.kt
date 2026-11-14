package com.yezdiconnect.app.obd

/**
 * Parses the text an ELM327-compatible adapter sends back (everything before its '>' prompt).
 *
 * Handles echo/whitespace noise, "SEARCHING...", error words, and both single-frame replies
 * ("41 0C 1A F8", with or without spaces) and CAN multi-frame replies:
 * ```
 * 014
 * 0: 49 02 01 4D 44 32
 * 1: 31 41 42 43 44 45 46
 * ```
 */
object ElmResponse {

    private val errors = listOf(
        "NO DATA", "UNABLE TO CONNECT", "CAN ERROR", "BUS ERROR", "BUS INIT", "STOPPED",
        "ERROR", "BUFFER FULL", "DATA ERROR", "FB ERROR", "LV RESET", "ACT ALERT", "?",
    )
    private val framePrefix = Regex("""^([0-9A-F]):\s*""")
    private val lengthLine = Regex("""^[0-9A-F]{3}$""")

    /** Meaningful lines, uppercased, without the prompt, blank lines or "SEARCHING...". */
    fun lines(raw: String): List<String> =
        raw.replace('\r', '\n').split('\n')
            .map { it.replace(">", "").trim().uppercase() }
            .filter { it.isNotEmpty() && !it.startsWith("SEARCHING") }

    /** The adapter's error word, if the reply is an error (e.g. "NO DATA" when the ECU is off). */
    fun error(lines: List<String>): String? =
        lines.firstNotNullOfOrNull { line -> errors.firstOrNull { line.startsWith(it) && !(it == "BUS INIT" && line.endsWith("OK")) } }

    /**
     * Reply frames as byte lists. CAN multi-frame parts ("0:", "1:", …) are joined into one frame;
     * every other line is its own frame (e.g. one per ECU, or K-line's several "43 …" lines).
     */
    fun frames(lines: List<String>): List<List<Int>> {
        val multi = lines.filter { framePrefix.containsMatchIn(it) }
        val singles = lines.filterNot { framePrefix.containsMatchIn(it) || lengthLine.matches(it) || error(listOf(it)) != null }
            .mapNotNull(::hexBytes)
        if (multi.isEmpty()) return singles
        val joined = multi
            .sortedBy { framePrefix.find(it)!!.groupValues[1].toInt(16) }
            .flatMap { hexBytes(it.replace(framePrefix, "")) ?: emptyList() }
        return listOf(joined) + singles
    }

    /** Data bytes of the first frame answering [mode]/[pid], e.g. mode 0x01 pid 0x0C → the A, B bytes. */
    fun payload(lines: List<String>, mode: Int, pid: Int): List<Int>? =
        frames(lines).firstOrNull { it.size >= 2 && it[0] == mode + 0x40 && it[1] == pid }?.drop(2)

    /** "41 0C 1A F8" or "410C1AF8" → [0x41, 0x0C, 0x1A, 0xF8]; null if not hex. */
    fun hexBytes(line: String): List<Int>? {
        val hex = line.replace(" ", "")
        if (hex.isEmpty() || hex.length % 2 != 0 || !hex.all { it in '0'..'9' || it in 'A'..'F' }) return null
        return hex.chunked(2).map { it.toInt(16) }
    }
}
