package com.wscanplus.core.threat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SsidFloodingHeuristicTest {
    private val heuristic = SsidFloodingHeuristic()

    private fun scan(
        bssid: String = "AA:BB:CC:DD:EE:FF",
        ssid: String = "TestNet",
    ): ScanInput =
        ScanInput(
            bssid = bssid,
            ssid = ssid,
            isHidden = false,
            capabilities = "[WPA2-PSK-CCMP]",
            rssiDbm = -50,
            frequencyMhz = 2412,
            channelWidth = 20,
            timestamp = System.currentTimeMillis(),
        )

    private fun context(
        count: Int,
        baseline: Int? = 20,
        stdDev: Double? = 5.0,
    ): ScanContext {
        val results: List<ScanInput> =
            (1..count).map { i: Int ->
                scan(bssid = "AA:BB:CC:DD:EE:%02X".format(i % 256), ssid = "Net$i")
            }
        return ScanContext(
            currentResults = results,
            knownProfiles = emptyMap(),
            baselineNetworkCount = baseline,
            baselineStdDev = stdDev,
            environmentType = EnvironmentType.PUBLIC,
        )
    }

    @Test
    fun `null baseline returns null`() {
        val ctx: ScanContext = context(count = 50, baseline = null, stdDev = 5.0)
        assertNull(heuristic.evaluate(ctx.currentResults[0], ctx))
    }

    @Test
    fun `null stddev returns null`() {
        val ctx: ScanContext = context(count = 50, baseline = 20, stdDev = null)
        assertNull(heuristic.evaluate(ctx.currentResults[0], ctx))
    }

    @Test
    fun `zero stddev returns null`() {
        val ctx: ScanContext = context(count = 50, baseline = 20, stdDev = 0.0)
        assertNull(heuristic.evaluate(ctx.currentResults[0], ctx))
    }

    @Test
    fun `z-score 2_4 returns null`() {
        // baseline=20, stdDev=5, need count where z=2.4 → count=32
        val ctx: ScanContext = context(count = 32, baseline = 20, stdDev = 5.0)
        assertNull(heuristic.evaluate(ctx.currentResults[0], ctx))
    }

    @Test
    fun `z-score 2_5 returns confidence 0_50`() {
        // baseline=20, stdDev=2, count=25 → z=(25-20)/2=2.5
        val ctx: ScanContext = context(count = 25, baseline = 20, stdDev = 2.0)
        val signal: ThreatSignal? = heuristic.evaluate(ctx.currentResults[0], ctx)
        assertNotNull(signal)
        assertEquals(0.50f, signal!!.confidence, 0.001f)
    }

    @Test
    fun `z-score 3_5 returns confidence 0_70`() {
        // baseline=20, stdDev=2, count=27 → z=(27-20)/2=3.5
        val ctx: ScanContext = context(count = 27, baseline = 20, stdDev = 2.0)
        val signal: ThreatSignal? = heuristic.evaluate(ctx.currentResults[0], ctx)
        assertNotNull(signal)
        assertEquals(0.70f, signal!!.confidence, 0.001f)
    }

    @Test
    fun `z-score 5_0 returns confidence 0_90`() {
        // baseline=20, stdDev=2, count=30 → z=(30-20)/2=5.0
        val ctx: ScanContext = context(count = 30, baseline = 20, stdDev = 2.0)
        val signal: ThreatSignal? = heuristic.evaluate(ctx.currentResults[0], ctx)
        assertNotNull(signal)
        assertEquals(0.90f, signal!!.confidence, 0.001f)
    }

    @Test
    fun `absolute count over 200 with low z-score returns 0_90`() {
        // baseline=190, stdDev=5, count=201 → z=(201-190)/5=2.2 (below 2.5) but count>200
        val ctx: ScanContext = context(count = 201, baseline = 190, stdDev = 5.0)
        val signal: ThreatSignal? = heuristic.evaluate(ctx.currentResults[0], ctx)
        assertNotNull(signal)
        assertEquals(0.90f, signal!!.confidence, 0.001f)
    }

    @Test
    fun `only emits for first input in context`() {
        val ctx: ScanContext = context(count = 30, baseline = 20, stdDev = 2.0)
        val signalFirst: ThreatSignal? = heuristic.evaluate(ctx.currentResults[0], ctx)
        val signalSecond: ThreatSignal? = heuristic.evaluate(ctx.currentResults[1], ctx)
        assertNotNull(signalFirst)
        assertNull(signalSecond)
    }

    @Test
    fun `reason contains count baseline and z-score`() {
        val ctx: ScanContext = context(count = 30, baseline = 20, stdDev = 2.0)
        val signal: ThreatSignal? = heuristic.evaluate(ctx.currentResults[0], ctx)
        assertNotNull(signal)
        val reason: String = signal!!.reasons[0]
        assertTrue(reason.contains("30"))
        assertTrue(reason.contains("20"))
        assertTrue(reason.contains("5.00"))
    }

    @Test
    fun `bssid is SCAN_LEVEL sentinel`() {
        val ctx: ScanContext = context(count = 30, baseline = 20, stdDev = 2.0)
        val signal: ThreatSignal? = heuristic.evaluate(ctx.currentResults[0], ctx)
        assertNotNull(signal)
        assertEquals("SCAN_LEVEL", signal!!.bssid)
    }
}
