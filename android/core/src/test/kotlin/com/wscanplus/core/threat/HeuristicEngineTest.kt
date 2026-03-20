package com.wscanplus.core.threat

import org.junit.Assert.assertEquals
import org.junit.Test

class HeuristicEngineTest {
    private val sampleInput =
        ScanInput(
            bssid = "AA:BB:CC:DD:EE:FF",
            ssid = "TestNet",
            isHidden = false,
            capabilities = "[WPA2-PSK-CCMP][ESS]",
            rssiDbm = -42,
            frequencyMhz = 2412,
            channelWidth = 20,
            timestamp = 123456L,
        )

    private val sampleContext =
        ScanContext(
            currentResults = listOf(sampleInput),
            knownProfiles = emptyMap(),
            baselineNetworkCount = null,
            baselineStdDev = null,
            environmentType = EnvironmentType.RESIDENTIAL,
        )

    @Test
    fun `empty heuristic list returns empty`() {
        val engine = HeuristicEngine(emptyList())
        val output = engine.analyze(sampleContext)
        assertEquals(emptyList<ThreatSignal>(), output)
    }

    @Test
    fun `single heuristic returns signal`() {
        val heuristic =
            FakeHeuristic(
                HeuristicType.WEP_OPEN,
                threatSignalFor(sampleInput.bssid, HeuristicType.WEP_OPEN),
            )
        val engine = HeuristicEngine(listOf(heuristic))
        val output = engine.analyze(sampleContext)
        assertEquals(1, output.size)
        assertEquals(HeuristicType.WEP_OPEN, output.first().heuristicType)
    }

    @Test
    fun `multiple heuristics combine signals`() {
        val a =
            FakeHeuristic(
                HeuristicType.WEP_OPEN,
                threatSignalFor(sampleInput.bssid, HeuristicType.WEP_OPEN),
            )
        val b =
            FakeHeuristic(
                HeuristicType.RSSI_ANOMALY,
                threatSignalFor(sampleInput.bssid, HeuristicType.RSSI_ANOMALY),
            )
        val engine = HeuristicEngine(listOf(a, b))
        val output = engine.analyze(sampleContext)
        assertEquals(2, output.size)
    }

    @Test
    fun `null heuristic results are excluded`() {
        val nullHeuristic = FakeHeuristic(HeuristicType.WEP_OPEN, null)
        val positiveHeuristic =
            FakeHeuristic(
                HeuristicType.RSSI_ANOMALY,
                threatSignalFor(sampleInput.bssid, HeuristicType.RSSI_ANOMALY),
            )
        val engine = HeuristicEngine(listOf(nullHeuristic, positiveHeuristic))
        val output = engine.analyze(sampleContext)
        assertEquals(1, output.size)
        assertEquals(HeuristicType.RSSI_ANOMALY, output.first().heuristicType)
    }

    private fun threatSignalFor(
        bssid: String,
        type: HeuristicType,
    ): ThreatSignal =
        ThreatSignal(
            confidence = 0.7f,
            source = ThreatSource.LOCAL_HEURISTIC,
            reasons = listOf("test"),
            heuristicType = type,
            bssid = bssid,
        )

    private class FakeHeuristic(
        override val type: HeuristicType,
        private val result: ThreatSignal?,
    ) : Heuristic {
        override fun evaluate(
            input: ScanInput,
            context: ScanContext,
        ): ThreatSignal? = result
    }
}
