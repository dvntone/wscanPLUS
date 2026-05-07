package com.wscanplus.app.nmea

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.Socket
import java.nio.charset.StandardCharsets

class UsbNmeaServerTest {
    // port = 0 lets the OS pick a free port, avoiding conflicts in CI.
    private var server = UsbNmeaServer(port = 0)

    @After
    fun tearDown() {
        server.stop()
    }

    @Test
    fun `start sets isRunning and exposes a valid bound port`() {
        server.start()

        assertTrue(server.isRunning)
        assertTrue(server.boundPort > 0)
        assertNull(server.lastError)
    }

    @Test
    fun `start is idempotent when already running`() {
        server.start()
        val port = server.boundPort

        server.start()

        assertTrue(server.isRunning)
        assertEquals(port, server.boundPort)
    }

    @Test
    fun `stop clears isRunning`() {
        server.start()
        server.stop()

        assertFalse(server.isRunning)
    }

    @Test
    fun `stop is safe when never started`() {
        server.stop() // must not throw
        assertFalse(server.isRunning)
    }

    @Test
    fun `start fails gracefully on unresolvable host`() {
        server = UsbNmeaServer(host = "invalid.host.xyz.invalid")
        server.start()

        assertFalse(server.isRunning)
        assertNotNull(server.lastError)
    }

    @Test
    fun `broadcast delivers payload to a connected client`() {
        server.start()
        val socket = Socket("127.0.0.1", server.boundPort)
        awaitCondition { server.clientCount == 1 }

        server.broadcast("\$GPRMC,test*00\r\n")

        val buf = ByteArray(32)
        socket.soTimeout = 2_000
        val n = socket.getInputStream().read(buf)
        assertTrue(n > 0)
        assertTrue(String(buf, 0, n, StandardCharsets.UTF_8).contains("GPRMC"))
        socket.close()
    }

    @Test
    fun `broadcast delivers to all connected clients`() {
        server.start()
        val port = server.boundPort
        val s1 = Socket("127.0.0.1", port)
        val s2 = Socket("127.0.0.1", port)
        awaitCondition { server.clientCount == 2 }

        server.broadcast("HELLO\r\n")

        for (s in listOf(s1, s2)) {
            s.soTimeout = 2_000
            val buf = ByteArray(16)
            val n = s.getInputStream().read(buf)
            assertEquals("HELLO\r\n", String(buf, 0, n, StandardCharsets.UTF_8))
            s.close()
        }
    }

    @Test
    fun `broadcast removes dead clients after write failure`() {
        server.start()
        val socket = Socket("127.0.0.1", server.boundPort)
        awaitCondition { server.clientCount == 1 }

        socket.close()
        // A broadcast attempt triggers the dead-client sweep.
        awaitCondition {
            server.broadcast("probe\r\n")
            server.clientCount == 0
        }

        assertEquals(0, server.clientCount)
    }

    private fun awaitCondition(
        timeoutMs: Long = 3_000,
        condition: () -> Boolean,
    ) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            Thread.sleep(20)
        }
        error("Timed out waiting for condition")
    }
}
