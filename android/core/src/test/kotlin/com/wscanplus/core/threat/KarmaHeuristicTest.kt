package com.wscanplus.core.threat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KarmaHeuristicTest {
    private val heuristic = KarmaHeuristic()

    private fun scan(
        bssid: String = "AA:BB:CC:DD:EE:FF",
        ssid: String = "Net1",
        isHidden: Boolean = false,
        rssiDbm: Int = -50,
        frequencyMhz: Int = 2412,
    ): ScanInput =
        ScanInput(
            bssid = bssid,
            ssid = ssid,
            isHidden = isHidden,
            capabilities = "[WPA2-PSK-CCMP]",
            rssiDbm = rssiDbm,
            frequencyMhz = frequencyMhz,
            channelWidth = 20,
            timestamp = System.currentTimeMillis(),
        )

    private fun context(results: List<ScanInput>): ScanContext =
        ScanContext(
            currentResults = results,
            knownProfiles = emptyMap(),
            baselineNetworkCount = null,
            baselineStdDev = null,
            environmentType = EnvironmentType.RESIDENTIAL,
        )

    @Test
    fun `2 SSIDs on same BSSID returns null`() {
        val results: List<ScanInput> = listOf(
            scan(ssid = "Net1", frequencyMhz = 2412),
            scan(ssid = "Net2", frequencyMhz = 5180),
        )
        val ctx: ScanContext = context(results)
        assertNull(heuristic.evaluate(results[0], ctx))
    }

    @Test
    fun `3 distinct SSIDs on same BSSID returns confidence 0_45`() {
        val results: List<ScanInput> = listOf(
            scan(ssid = "Net1", frequencyMhz = 2412),
            scan(ssid = "Net2", frequencyMhz = 5180),
            scan(ssid = "Net3", frequencyMhz = 2437),
        )
        val ctx: ScanContext = context(results)
        val signal: ThreatSignal? = heuristic.evaluate(results[0], ctx)
        assertNotNull(signal)
        assertEquals(0.45f, signal!!.confidence, 0.001f)
    }

    @Test
    fun `5 distinct SSIDs returns confidence 0_75`() {
        val results: List<ScanInput> = (1..5).map { i: Int ->
            scan(ssid = "Net$i", frequencyMhz = 2412 + i * 5)
        }
        val ctx: ScanContext = context(results)
        val signal: ThreatSignal? = heuristic.evaluate(results[0], ctx)
        assertNotNull(signal)
        assertEquals(0.75f, signal!!.confidence, 0.001f)
    }

    @Test
    fun `10 distinct SSIDs returns confidence 0_95`() {
        val results: List<ScanInput> = (1..10).map { i: Int ->
            scan(ssid = "Net$i", frequencyMhz = 2412 + i * 5)
        }
        val ctx: ScanContext = context(results)
        val signal: ThreatSignal? = heuristic.evaluate(results[0], ctx)
        assertNotNull(signal)
        assertEquals(0.95f, signal!!.confidence, 0.001f)
    }

    @Test
    fun `hidden SSIDs excluded from count`() {
        val results: List<ScanInput> = listOf(
            scan(ssid = "Net1", frequencyMhz = 2412),
            scan(ssid = "Net2", frequencyMhz = 5180),
            scan(ssid = "Net3", frequencyMhz = 2437),
            scan(ssid = "Hidden1", isHidden = true, frequencyMhz = 5200),
            scan(ssid = "Hidden2", isHidden = true, frequencyMhz = 5220),
        )
        val ctx: ScanContext = context(results)
        val signal: ThreatSignal? = heuristic.evaluate(results[0], ctx)
        assertNotNull(signal)
        assertEquals(0.45f, signal!!.confidence, 0.001f)
    }

    @Test
    fun `same channel and tight RSSI applies bonus`() {
        val results: List<ScanInput> = listOf(
            scan(ssid = "Net1", rssiDbm = -50, frequencyMhz = 2412),
            scan(ssid = "Net2", rssiDbm = -48, frequencyMhz = 2412),
            scan(ssid = "Net3", rssiDbm = -52, frequencyMhz = 2412),
        )
        val ctx: ScanContext = context(results)
        val signal: ThreatSignal? = heuristic.evaluate(results[0], ctx)
        assertNotNull(signal)
        assertEquals(0.55f, signal!!.confidence, 0.001f)
        assertTrue(signal.reasons.any { reason: String -> reason.contains("RSSI cluster") })
    }

    @Test
    fun `wide RSSI spread no bonus`() {
        val results: List<ScanInput> = listOf(
            scan(ssid = "Net1", rssiDbm = -30, frequencyMhz = 2412),
            scan(ssid = "Net2", rssiDbm = -50, frequencyMhz = 2412),
            scan(ssid = "Net3", rssiDbm = -70, frequencyMhz = 2412),
        )
        val ctx: ScanContext = context(results)
        val signal: ThreatSignal? = heuristic.evaluate(results[0], ctx)
        assertNotNull(signal)
        assertEquals(0.45f, signal!!.confidence, 0.001f)
        assertEquals(1, signal.reasons.size)
    }

    @Test
    fun `multiple qualifying BSSIDs produce separate signals`() {
        val bssid1 = "AA:AA:AA:AA:AA:AA"
        val bssid2 = "BB:BB:BB:BB:BB:BB"
        val results: List<ScanInput> = listOf(
            scan(bssid = bssid1, ssid = "A1"),
            scan(bssid = bssid1, ssid = "A2"),
            scan(bssid = bssid1, ssid = "A3"),
            scan(bssid = bssid2, ssid = "B1"),
            scan(bssid = bssid2, ssid = "B2"),
            scan(bssid = bssid2, ssid = "B3"),
        )
        val ctx: ScanContext = context(results)
        val signal1: ThreatSignal? = heuristic.evaluate(results[0], ctx)
        val signal2: ThreatSignal? = heuristic.evaluate(results[3], ctx)
        assertNotNull(signal1)
        assertNotNull(signal2)
        assertEquals(bssid1, signal1!!.bssid)
        assertEquals(bssid2, signal2!!.bssid)
    }

    @Test
    fun `duplicate SSIDs on same BSSID counted once`() {
        val results: List<ScanInput> = listOf(
            scan(ssid = "Net1", frequencyMhz = 2412),
            scan(ssid = "Net1", frequencyMhz = 5180),
            scan(ssid = "Net2", frequencyMhz = 2437),
            scan(ssid = "Net3", frequencyMhz = 5200),
        )
        val ctx: ScanContext = context(results)
        val signal: ThreatSignal? = heuristic.evaluate(results[0], ctx)
        assertNotNull(signal)
        assertEquals(0.45f, signal!!.confidence, 0.001f)
        assertTrue(signal.reasons[0].contains("3 distinct SSIDs"))
    }

    @Test
    fun `non-first entry for same BSSID returns null`() {
        val results: List<ScanInput> = listOf(
            scan(ssid = "Net1", frequencyMhz = 2412),
            scan(ssid = "Net2", frequencyMhz = 5180),
            scan(ssid = "Net3", frequencyMhz = 2437),
        )
        val ctx: ScanContext = context(results)
        assertNull(heuristic.evaluate(results[1], ctx))
    }

    @Test
    fun `bonus capped at 0_95 for 10 SSIDs with tight cluster`() {
        val results: List<ScanInput> = (1..10).map { i: Int ->
            scan(ssid = "Net$i", rssiDbm = -50, frequencyMhz = 2412)
        }
        val ctx: ScanContext = context(results)
        val signal: ThreatSignal? = heuristic.evaluate(results[0], ctx)
        assertNotNull(signal)
        assertEquals(0.95f, signal!!.confidence, 0.001f)
    }
}
