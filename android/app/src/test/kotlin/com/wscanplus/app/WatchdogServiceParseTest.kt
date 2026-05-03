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

    @Test
    fun `valid ack message updates lastDesktopAckSeq`() {
        val service = WatchdogService()

        invokeParseDesktopMessage(service, """{"type":"ack","seq":9}""")

        assertEquals(9, service.lastDesktopAckSeq)
    }

    @Test
    fun `malformed desktop message does not crash or update ack`() {
        val service = WatchdogService()

        invokeParseDesktopMessage(service, "not json")

        assertEquals(-1, service.lastDesktopAckSeq)
    }

    @Test
    fun `non-ack desktop message is ignored`() {
        val service = WatchdogService()

        invokeParseDesktopMessage(service, """{"type":"ping","seq":2}""")

        assertEquals(-1, service.lastDesktopAckSeq)
    }

    private fun invokeParseDesktopMessage(
        service: WatchdogService,
        line: String,
    ) {
        val method =
            WatchdogService::class.java.getDeclaredMethod(
                "parseDesktopMessage",
                String::class.java,
            )
        method.isAccessible = true
        method.invoke(service, line)
    }
}
