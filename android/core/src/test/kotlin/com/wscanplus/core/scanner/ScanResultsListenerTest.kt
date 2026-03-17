package com.wscanplus.core.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanResultsListenerTest {
    @Test
    fun `listener receives results via lambda`() {
        val received = mutableListOf<List<WifiScanResult>>()
        val listener = ScanResultsListener { results -> received.add(results) }

        val testResults =
            listOf(
                WifiScanResult(
                    ssid = "Net1",
                    bssid = "AA:BB:CC:DD:EE:01",
                    signalLevel = -50,
                    frequencyMhz = 2412,
                    capabilities = "[WPA2-PSK]",
                    timestamp = 100L,
                    channelWidth = 0,
                    centerFreq0 = 2412,
                    centerFreq1 = 0,
                ),
            )

        listener.onResults(testResults)
        assertEquals(1, received.size)
        assertEquals("Net1", received[0][0].ssid)
    }

    @Test
    fun `empty results list does not throw`() {
        val listener = ScanResultsListener { _ -> }
        listener.onResults(emptyList())
    }

    @Test
    fun `listener called multiple times accumulates calls`() {
        var callCount = 0
        val listener = ScanResultsListener { _ -> callCount++ }

        listener.onResults(emptyList())
        listener.onResults(emptyList())
        listener.onResults(emptyList())

        assertEquals(3, callCount)
    }

    @Test
    fun `no-op listener compiles and runs`() {
        val listener = ScanResultsListener { }
        listener.onResults(emptyList())
        assertTrue(true)
    }
}
