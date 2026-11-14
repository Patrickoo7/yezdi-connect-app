package com.yezdiconnect.app.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class BikeUuidsTest {
    @Test fun `all 22 uuids from the official app`() {
        assertEquals(22, BikeUuids.all.size)
        assertEquals(22, BikeUuids.all.toSet().size)
        assertEquals(UUID.fromString("d6328aea-d630-4a83-b51b-1da8e8da8100"), BikeUuids.all.first())
        assertEquals(UUID.fromString("d6328aea-d630-4a83-b51b-1da8e8da8620"), BikeUuids.all.last())
    }

    @Test fun `recognises the vendor family`() {
        assertTrue(BikeUuids.isBikeUuid(UUID.fromString("D6328AEA-D630-4A83-B51B-1DA8E8DA8410")))
        assertFalse(BikeUuids.isBikeUuid(UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")))
    }
}
