package com.wscanplus.core.threat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RssiAnomalyHeuristicTest {
    private val heuristic = RssiAnomalyHeuristic()

    private fun scan(
        bssid: String = "AA:BB:CC:DD:EE:FF",
        ssid: String = "TestNet",
        rssiDbm: Int = -50,
    ): ScanInput =
        ScanInput(
            bssid = bssid,
            ssid = ssid,
            isHidden = false,
            capabilities = "[WPA2-PSK-CCMP]",
            rssiDbm = rssiDbm,
            frequencyMhz = 2412,
            channelWidth = 20,
            timestamp = System.currentTimeMillis(),
        )

    private fun context(
        environmentType: EnvironmentType = EnvironmentType.RESIDENTIAL,
        knownBssids: Set<String> = emptySet(),
        input: ScanInput,
    ): ScanContext {
        val profiles: Map<String, BssidProfile> =
            knownBssids.associateWith { bssid: String ->
                BssidProfile(
                    bssid = bssid,
                    firstSeenAt = System.currentTimeMillis() - 10_000L,
                    lastSeenAt = System.currentTimeMillis(),
                    ssids = setOf(input.ssid),
                    capabilitiesHistory = listOf("[WPA2-PSK-CCMP]"),
                    observationCount = 10,
                    ouiVendor = null,
                )
            }
        return ScanContext(
            currentResults = listOf(input),
            knownProfiles = profiles,
            baselineNetworkCount = null,
            baselineStdDev = null,
            environmentType = environmentType,
        )
    }

    @Test
    fun `new network at -19dBm RESIDENTIAL returns 0_80`() {
        val input: ScanInput = scan(rssiDbm = -19)
        val ctx: ScanContext = context(input = input)
        val signal: ThreatSignal? = heuristic.evaluate(input, ctx)
        assertNotNull(signal)
        assertEquals(0.80f, signal!!.confidence, 0.001f)
    }

    @Test
    fun `new network at -24dBm RESIDENTIAL returns 0_60`() {
        val input: ScanInput = scan(rssiDbm = -24)
        val ctx: ScanContext = context(input = input)
        val signal: ThreatSignal? = heuristic.evaluate(input, ctx)
        assertNotNull(signal)
        assertEquals(0.60f, signal!!.confidence, 0.001f)
    }

    @Test
    fun `new network at -29dBm RESIDENTIAL returns 0_45`() {
        val input: ScanInput = scan(rssiDbm = -29)
        val ctx: ScanContext = context(input = input)
        val signal: ThreatSignal? = heuristic.evaluate(input, ctx)
        assertNotNull(signal)
        assertEquals(0.45f, signal!!.confidence, 0.001f)
    }

    @Test
    fun `new network at -31dBm RESIDENTIAL returns null`() {
        val input: ScanInput = scan(rssiDbm = -31)
        val ctx: ScanContext = context(input = input)
        assertNull(heuristic.evaluate(input, ctx))
    }

    @Test
    fun `known network at -19dBm RESIDENTIAL returns 0_30`() {
        val input: ScanInput = scan(rssiDbm = -19)
        val ctx: ScanContext = context(knownBssids = setOf(input.bssid), input = input)
        val signal: ThreatSignal? = heuristic.evaluate(input, ctx)
        assertNotNull(signal)
        assertEquals(0.30f, signal!!.confidence, 0.001f)
    }

    @Test
    fun `known network at -24dBm RESIDENTIAL returns 0_15`() {
        val input: ScanInput = scan(rssiDbm = -24)
        val ctx: ScanContext = context(knownBssids = setOf(input.bssid), input = input)
        val signal: ThreatSignal? = heuristic.evaluate(input, ctx)
        assertNotNull(signal)
        assertEquals(0.15f, signal!!.confidence, 0.001f)
    }

    @Test
    fun `known network at -29dBm RESIDENTIAL returns null`() {
        val input: ScanInput = scan(rssiDbm = -29)
        val ctx: ScanContext = context(knownBssids = setOf(input.bssid), input = input)
        assertNull(heuristic.evaluate(input, ctx))
    }

    @Test
    fun `OFFICE environment threshold at -26dBm returns null and -24dBm returns signal`() {
        val inputBelow: ScanInput = scan(rssiDbm = -26)
        val ctxBelow: ScanContext = context(environmentType = EnvironmentType.OFFICE, input = inputBelow)
        assertNull(heuristic.evaluate(inputBelow, ctxBelow))

        val inputAbove: ScanInput = scan(rssiDbm = -24)
        val ctxAbove: ScanContext = context(environmentType = EnvironmentType.OFFICE, input = inputAbove)
        val signal: ThreatSignal? = heuristic.evaluate(inputAbove, ctxAbove)
        assertNotNull(signal)
    }

    @Test
    fun `PUBLIC environment threshold at -21dBm returns null and -19dBm returns signal`() {
        val inputBelow: ScanInput = scan(rssiDbm = -21)
        val ctxBelow: ScanContext = context(environmentType = EnvironmentType.PUBLIC, input = inputBelow)
        assertNull(heuristic.evaluate(inputBelow, ctxBelow))

        val inputAbove: ScanInput = scan(rssiDbm = -19)
        val ctxAbove: ScanContext = context(environmentType = EnvironmentType.PUBLIC, input = inputAbove)
        val signal: ThreatSignal? = heuristic.evaluate(inputAbove, ctxAbove)
        assertNotNull(signal)
    }

    @Test
    fun `heuristic type is RSSI_ANOMALY`() {
        val input: ScanInput = scan(rssiDbm = -19)
        val ctx: ScanContext = context(input = input)
        val signal: ThreatSignal? = heuristic.evaluate(input, ctx)
        assertNotNull(signal)
        assertEquals(HeuristicType.RSSI_ANOMALY, signal!!.heuristicType)
    }
}
