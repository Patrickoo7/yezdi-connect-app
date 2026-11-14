package com.yezdiconnect.app.protocol

import java.util.UUID

/**
 * Bluetooth LE identifiers observed in the official app (REA static analysis, see README).
 *
 * All 22 UUIDs share one vendor base and differ only in the last 3 hex digits. They form six
 * groups (0x81xx … 0x86xx); each group is most likely one GATT service plus its characteristics.
 * Which one carries which reading or dash command is NOT known yet: that comes from the
 * nRF Connect scan and the HCI snoop log described in docs/YOUR-TASKS.txt.
 */
object BikeUuids {
    private const val BASE_PREFIX = "d6328aea-d630-4a83-b51b-1da8e8da8"

    fun of(suffix: Int): UUID = UUID.fromString(BASE_PREFIX + "%03x".format(suffix))

    /** Every UUID seen in the official app, grouped by their 0x100 block. */
    val groups: Map<Int, List<UUID>> = mapOf(
        0x100 to listOf(0x100, 0x110),
        0x200 to listOf(0x200, 0x210, 0x220, 0x230),
        0x300 to listOf(0x300, 0x310),
        0x400 to listOf(0x400, 0x410, 0x420, 0x430, 0x440, 0x450, 0x460, 0x470, 0x480),
        0x500 to listOf(0x500, 0x510),
        0x600 to listOf(0x600, 0x610, 0x620),
    ).mapValues { (_, suffixes) -> suffixes.map(::of) }

    val all: List<UUID> = groups.values.flatten()

    /** True when [uuid] belongs to the bike's vendor family. Used to recognise the bike in scans. */
    fun isBikeUuid(uuid: UUID): Boolean = uuid.toString().lowercase().startsWith(BASE_PREFIX)
}
