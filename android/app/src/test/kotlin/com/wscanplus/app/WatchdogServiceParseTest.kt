package com.wscanplus.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WatchdogServiceParseTest {
    @Test
    fun `valid ack line returns envelope with type and seq`() {
        val envelope = parseDesktopMessageLine("""{"type":"ack","seq":5}""")
        assertEquals("ack", envelope?.type)
        assertEquals(5, envelope?.seq)
    }

    @Test
    fun `seq defaults to minus one when absent`() {
        val envelope = parseDesktopMessageLine("""{"type":"ack"}""")
        assertEquals(-1, envelope?.seq)
    }

    @Test
    fun `non-ack type is parsed without error`() {
        val envelope = parseDesktopMessageLine("""{"type":"ping","seq":0}""")
        assertEquals("ping", envelope?.type)
    }

    @Test
    fun `unknown fields are ignored`() {
        val envelope = parseDesktopMessageLine("""{"type":"ack","seq":3,"extra":"ignored"}""")
        assertEquals("ack", envelope?.type)
        assertEquals(3, envelope?.seq)
    }

    @Test
    fun `malformed JSON returns null`() {
        assertNull(parseDesktopMessageLine("not json"))
    }

    @Test
    fun `empty string returns null`() {
        assertNull(parseDesktopMessageLine(""))
    }

    @Test
    fun `missing type field returns null`() {
        assertNull(parseDesktopMessageLine("""{"seq":1}"""))
    }
}
