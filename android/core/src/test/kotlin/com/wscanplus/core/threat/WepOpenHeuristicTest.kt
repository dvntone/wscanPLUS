package com.wscanplus.core.threat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WepOpenHeuristicTest {
    private val heuristic = WepOpenHeuristic()

    private fun scanInput(
        capabilities: String,
        bssid: String = "AA:BB:CC:DD:EE:FF",
    ): ScanInput =
        ScanInput(
            bssid = bssid,
            ssid = "TestNetwork",
            isHidden = false,
            capabilities = capabilities,
            rssiDbm = -50,
            frequencyMhz = 2412,
            channelWidth = 20,
            timestamp = System.currentTimeMillis(),
        )

    private val emptyContext =
        ScanContext(
            currentResults = emptyList(),
            knownProfiles = emptyMap(),
            baselineNetworkCount = null,
            baselineStdDev = null,
            environmentType = EnvironmentType.RESIDENTIAL,
        )

    @Test
    fun `open network returns confidence 0_4`() {
        val signal: ThreatSignal? = heuristic.evaluate(scanInput("[ESS]"), emptyContext)
        assertNotNull(signal)
        assertEquals(0.4f, signal!!.confidence)
        assertTrue(signal.reasons[0].contains("no encryption"))
    }

    @Test
    fun `wep network returns confidence 0_6`() {
        val signal: ThreatSignal? = heuristic.evaluate(scanInput("[WEP]"), emptyContext)
        assertNotNull(signal)
        assertEquals(0.6f, signal!!.confidence)
        assertTrue(signal.reasons[0].contains("crackable"))
    }

    @Test
    fun `wpa network returns confidence 0_3`() {
        val signal: ThreatSignal? = heuristic.evaluate(scanInput("[WPA-PSK-TKIP]"), emptyContext)
        assertNotNull(signal)
        assertEquals(0.3f, signal!!.confidence)
        assertTrue(signal.reasons[0].contains("deprecated"))
    }

    @Test
    fun `wpa2 network returns null`() {
        val signal: ThreatSignal? =
            heuristic.evaluate(scanInput("[WPA2-PSK-CCMP][RSN-PSK-CCMP]"), emptyContext)
        assertNull(signal)
    }

    @Test
    fun `wpa3 network returns null`() {
        val signal: ThreatSignal? = heuristic.evaluate(scanInput("[SAE][RSN-SAE-CCMP]"), emptyContext)
        assertNull(signal)
    }

    @Test
    fun `owe enhanced open returns null`() {
        val signal: ThreatSignal? = heuristic.evaluate(scanInput("[RSN-OWE-CCMP]"), emptyContext)
        assertNull(signal)
    }

    @Test
    fun `signals have correct source and heuristic type`() {
        val signal: ThreatSignal? = heuristic.evaluate(scanInput("[ESS]"), emptyContext)
        assertNotNull(signal)
        assertEquals(ThreatSource.LOCAL_HEURISTIC, signal!!.source)
        assertEquals(HeuristicType.WEP_OPEN, signal.heuristicType)
    }

    @Test
    fun `signals have exactly one reason`() {
        val signal: ThreatSignal? = heuristic.evaluate(scanInput("[WEP]"), emptyContext)
        assertNotNull(signal)
        assertEquals(1, signal!!.reasons.size)
    }

    @Test
    fun `signal contains correct bssid`() {
        val bssid = "11:22:33:44:55:66"
        val signal: ThreatSignal? =
            heuristic.evaluate(scanInput("[ESS]", bssid = bssid), emptyContext)
        assertNotNull(signal)
        assertEquals(bssid, signal!!.bssid)
    }
}
