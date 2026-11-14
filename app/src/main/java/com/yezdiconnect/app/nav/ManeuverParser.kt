package com.yezdiconnect.app.nav

/**
 * Turns the text of Google Maps' navigation notification into a [Maneuver].
 *
 * Google Maps puts the distance to the next step in one line (usually the title, e.g. "350 m")
 * and the instruction in another (e.g. "Turn left onto MG Road"). Which line holds what has
 * changed between app versions, so every line is searched for both.
 *
 * English only for now. The turn arrow itself is a bitmap; when text gives no turn direction,
 * the type is [ManeuverType.UNKNOWN] and the icon classifier (added once reference icons are
 * captured on a real phone) can fill it in.
 */
object ManeuverParser {

    private val distanceRegex = Regex(
        """(?<![\w.])(\d+(?:[.,]\d+)?)\s*(km|m|mi|ft|yd)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val roadRegex = Regex("""\b(?:onto|on to|toward|towards|on|into|via)\s+(.+)$""", RegexOption.IGNORE_CASE)
    private val exitRegex = Regex("""\b(\d+)(?:st|nd|rd|th)\s+exit\b""", RegexOption.IGNORE_CASE)
    private val wordExits = listOf("first", "second", "third", "fourth", "fifth", "sixth", "seventh", "eighth")

    /** Ordered: more specific phrases must come before the general ones they contain. */
    private val typeRules: List<Pair<Regex, ManeuverType>> = listOf(
        "u-?turn|make a u" to ManeuverType.U_TURN,
        "roundabout|rotary|traffic circle" to ManeuverType.ROUNDABOUT,
        "arriv|destination|you have reached|is on the (left|right)" to ManeuverType.DESTINATION,
        "sharp left" to ManeuverType.SHARP_LEFT,
        "sharp right" to ManeuverType.SHARP_RIGHT,
        "slight left|bear left" to ManeuverType.SLIGHT_LEFT,
        "slight right|bear right" to ManeuverType.SLIGHT_RIGHT,
        "keep left|stay left|fork left|take the left fork|use the left" to ManeuverType.KEEP_LEFT,
        "keep right|stay right|fork right|take the right fork|use the right" to ManeuverType.KEEP_RIGHT,
        "merge" to ManeuverType.MERGE,
        "turn left|left turn|\\bleft onto\\b" to ManeuverType.TURN_LEFT,
        "turn right|right turn|\\bright onto\\b" to ManeuverType.TURN_RIGHT,
        "continue|head|straight|go straight|stay on" to ManeuverType.STRAIGHT,
    ).map { (pattern, type) -> Regex(pattern, RegexOption.IGNORE_CASE) to type }

    /**
     * @param lines the notification's text fields (title, text, big text, sub text), any may be null.
     * @return null when no line looks like a navigation step.
     */
    fun parse(vararg lines: String?): Maneuver? {
        val texts = lines.filterNotNull().map { it.trim() }.filter { it.isNotEmpty() }
        if (texts.isEmpty()) return null

        // The sub text ("12 min · 4.5 km · 10:42 ETA") also contains a distance, so prefer
        // the first line whose distance is not part of a "·"-separated trip summary.
        val distance = texts.firstNotNullOfOrNull { line ->
            if (line.contains('·') || line.contains("ETA", ignoreCase = true)) null else parseDistance(line)
        }

        val instruction = texts.firstOrNull { line -> typeRules.any { (r, _) -> r.containsMatchIn(line) } }
        val type = instruction?.let { line -> typeRules.first { (r, _) -> r.containsMatchIn(line) }.second }
            ?: ManeuverType.UNKNOWN

        if (distance == null && instruction == null) return null

        val instructionText = instruction ?: texts.first()
        return Maneuver(
            type = type,
            distance = distance,
            road = instruction?.let(::parseRoad),
            roundaboutExit = if (type == ManeuverType.ROUNDABOUT) parseExit(instructionText) else null,
            instruction = instructionText,
        )
    }

    fun parseDistance(text: String): Distance? {
        val match = distanceRegex.find(text) ?: return null
        val value = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
        val unit = when (match.groupValues[2].lowercase()) {
            "km" -> DistanceUnit.KILOMETERS
            "m" -> DistanceUnit.METERS
            "mi" -> DistanceUnit.MILES
            "ft" -> DistanceUnit.FEET
            "yd" -> DistanceUnit.YARDS
            else -> return null
        }
        return Distance(value, unit)
    }

    private fun parseRoad(instruction: String): String? {
        // Drop a leading distance ("In 200 m, turn left onto X") before looking for the road.
        val text = instruction.substringAfter(", ", instruction)
        return roadRegex.find(text)?.groupValues?.get(1)?.trim()?.trimEnd('.')?.takeIf { it.isNotEmpty() }
    }

    private fun parseExit(instruction: String): Int? {
        exitRegex.find(instruction)?.let { return it.groupValues[1].toIntOrNull() }
        val lower = instruction.lowercase()
        wordExits.forEachIndexed { i, word -> if ("$word exit" in lower) return i + 1 }
        return null
    }
}
