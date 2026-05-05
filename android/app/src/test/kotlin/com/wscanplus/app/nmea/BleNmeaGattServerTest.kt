package com.wscanplus.app.nmea

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.charset.StandardCharsets

class BleNmeaGattServerTest {
    @Test
    fun `chunkUtf8 keeps whole payload when it fits mtu`() {
        val chunks = BleNmeaGattServer.chunkUtf8("\$GPRMC,test*00\r\n", 32)

        assertEquals(1, chunks.size)
        assertArrayEquals("\$GPRMC,test*00\r\n".toByteArray(StandardCharsets.UTF_8), chunks.single())
    }

    @Test
    fun `chunkUtf8 splits payload by mtu minus overhead`() {
        val payload = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"

        val chunks = BleNmeaGattServer.chunkUtf8(payload, 10)

        assertEquals(4, chunks.size)
        assertEquals("ABCDEFG", String(chunks[0], StandardCharsets.UTF_8))
        assertEquals("HIJKLMN", String(chunks[1], StandardCharsets.UTF_8))
        assertEquals("OPQRSTU", String(chunks[2], StandardCharsets.UTF_8))
        assertEquals("VWXYZ", String(chunks[3], StandardCharsets.UTF_8))
    }
}
