package com.wscanplus.core.threat

import org.junit.Assert.assertEquals
import org.junit.Test

class PolicyGateTest {
    private val sampleSignalLow =
        ThreatSignal(
            confidence = 0.2f,
            source = ThreatSource.LOCAL_HEURISTIC,
            reasons = listOf("low"),
            heuristicType = HeuristicType.WEP_OPEN,
            bssid = "AA:BB:CC:DD:EE:01",
        )

    private val sampleSignalMid =
        ThreatSignal(
            confidence = 0.5f,
            source = ThreatSource.LOCAL_HEURISTIC,
            reasons = listOf("mid"),
            heuristicType = HeuristicType.WEP_OPEN,
            bssid = "AA:BB:CC:DD:EE:02",
        )

    private val sampleSignalHigh =
        ThreatSignal(
            confidence = 0.8f,
            source = ThreatSource.LOCAL_HEURISTIC,
            reasons = listOf("high"),
            heuristicType = HeuristicType.WEP_OPEN,
            bssid = "AA:BB:CC:DD:EE:03",
        )

    @Test
    fun `default config filters signals below point three`() {
        val gate = PolicyGate()
        val filtered = gate.filter(listOf(sampleSignalLow, sampleSignalMid, sampleSignalHigh))
        assertEquals(listOf(sampleSignalMid, sampleSignalHigh), filtered)
    }

    @Test
    fun `custom threshold filters correctly`() {
        val gate = PolicyGate(PolicyConfig(minimumConfidence = 0.7f))
        val filtered = gate.filter(listOf(sampleSignalLow, sampleSignalMid, sampleSignalHigh))
        assertEquals(listOf(sampleSignalHigh), filtered)
    }

    @Test
    fun `empty list returns empty`() {
        val gate = PolicyGate()
        val filtered = gate.filter(emptyList())
        assertEquals(emptyList<ThreatSignal>(), filtered)
    }

    @Test
    fun `all filtered out returns empty`() {
        val gate = PolicyGate(PolicyConfig(minimumConfidence = 0.9f))
        val filtered = gate.filter(listOf(sampleSignalLow, sampleSignalMid, sampleSignalHigh))
        assertEquals(emptyList<ThreatSignal>(), filtered)
    }

    @Test
    fun `all pass through`() {
        val gate = PolicyGate(PolicyConfig(minimumConfidence = 0.1f))
        val filtered = gate.filter(listOf(sampleSignalLow, sampleSignalMid, sampleSignalHigh))
        assertEquals(listOf(sampleSignalLow, sampleSignalMid, sampleSignalHigh), filtered)
    }
}
