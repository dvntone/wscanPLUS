package com.wscanplus.core.threat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ThreatSignalTest {
    private val signal =
        ThreatSignal(
            confidence = 0.75f,
            source = ThreatSource.LOCAL_HEURISTIC,
            reasons = listOf("WEP detected"),
            heuristicType = HeuristicType.WEP_OPEN,
            bssid = "AA:BB:CC:DD:EE:FF",
            detectedAt = 1000L,
        )

    @Test
    fun `confidence at lower bound passes`() {
        val low = signal.copy(confidence = 0.0f)
        assertEquals(0.0f, low.confidence, 0.001f)
    }

    @Test
    fun `confidence at upper bound passes`() {
        val high = signal.copy(confidence = 0.95f)
        assertEquals(0.95f, high.confidence, 0.001f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `confidence above 0_95 throws`() {
        signal.copy(confidence = 0.96f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `confidence below 0_0 throws`() {
        signal.copy(confidence = -0.01f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `more than 3 reasons throws`() {
        signal.copy(reasons = listOf("a", "b", "c", "d"))
    }

    @Test
    fun `schemaVersion defaults to 1`() {
        val defaultSchema =
            ThreatSignal(
                confidence = 0.5f,
                source = ThreatSource.LOCAL_HEURISTIC,
                reasons = listOf("test"),
                heuristicType = null,
                bssid = "00:11:22:33:44:55",
            )
        assertEquals(1, defaultSchema.schemaVersion)
    }

    @Test
    fun `heuristicType nullable for CTI signals`() {
        val cti = signal.copy(source = ThreatSource.CROWDSEC_CTI, heuristicType = null)
        assertNull(cti.heuristicType)
    }

    @Test
    fun `heuristicType nullable for Gemini signals`() {
        val gemini = signal.copy(source = ThreatSource.GEMINI, heuristicType = null)
        assertNull(gemini.heuristicType)
    }

    @Test
    fun `data class equality on matching fields`() {
        val copy = signal.copy()
        assertEquals(signal, copy)
        assertEquals(signal.hashCode(), copy.hashCode())
    }

    @Test
    fun `SecurityType rank values are correct`() {
        assertEquals(4, SecurityType.WPA3.rank)
        assertEquals(3, SecurityType.OWE.rank)
        assertEquals(3, SecurityType.WPA2.rank)
        assertEquals(2, SecurityType.WPA.rank)
        assertEquals(1, SecurityType.WEP.rank)
        assertEquals(0, SecurityType.OPEN.rank)
    }
}
