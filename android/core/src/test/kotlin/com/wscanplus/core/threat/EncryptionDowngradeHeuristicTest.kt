package com.wscanplus.core.threat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EncryptionDowngradeHeuristicTest {
    private val heuristic = EncryptionDowngradeHeuristic()

    private fun scan(
        bssid: String = "AA:BB:CC:DD:EE:FF",
        capabilities: String = "[ESS]",
    ): ScanInput =
        ScanInput(
            bssid = bssid,
            ssid = "TestNet",
            isHidden = false,
            capabilities = capabilities,
            rssiDbm = -50,
            frequencyMhz = 2412,
            channelWidth = 20,
            timestamp = System.currentTimeMillis(),
        )

    private fun profile(
        bssid: String = "AA:BB:CC:DD:EE:FF",
        capabilitiesHistory: List<String> = listOf("[WPA2-PSK-CCMP]"),
        observationCount: Int = 10,
        daySpan: Int = 5,
    ): BssidProfile {
        val now: Long = System.currentTimeMillis()
        return BssidProfile(
            bssid = bssid,
            firstSeenAt = now - daySpan.toLong() * 86_400_000L,
            lastSeenAt = now,
            ssids = setOf("TestNet"),
            capabilitiesHistory = capabilitiesHistory,
            observationCount = observationCount,
            ouiVendor = null,
        )
    }

    private fun context(
        results: List<ScanInput> = emptyList(),
        profiles: Map<String, BssidProfile> = emptyMap(),
    ): ScanContext =
        ScanContext(
            currentResults = results,
            knownProfiles = profiles,
            baselineNetworkCount = null,
            baselineStdDev = null,
            environmentType = EnvironmentType.RESIDENTIAL,
        )

    @Test
    fun `wpa2 to open returns 0_85`() {
        val input: ScanInput = scan(capabilities = "[ESS]")
        val p: BssidProfile = profile(capabilitiesHistory = listOf("[WPA2-PSK-CCMP]"))
        val signal: ThreatSignal? = heuristic.evaluate(input, context(profiles = mapOf(input.bssid to p)))
        assertNotNull(signal)
        assertEquals(0.85f, signal!!.confidence, 0.001f)
    }

    @Test
    fun `wpa3 to wep returns 0_75`() {
        val input: ScanInput = scan(capabilities = "[WEP]")
        val p: BssidProfile = profile(capabilitiesHistory = listOf("[RSN-SAE-CCMP]"))
        val signal: ThreatSignal? = heuristic.evaluate(input, context(profiles = mapOf(input.bssid to p)))
        assertNotNull(signal)
        assertEquals(0.75f, signal!!.confidence, 0.001f)
    }

    @Test
    fun `wpa to open returns 0_5`() {
        val input: ScanInput = scan(capabilities = "[ESS]")
        val p: BssidProfile = profile(capabilitiesHistory = listOf("[WPA-PSK-TKIP]"))
        val signal: ThreatSignal? = heuristic.evaluate(input, context(profiles = mapOf(input.bssid to p)))
        assertNotNull(signal)
        assertEquals(0.5f, signal!!.confidence, 0.001f)
    }

    @Test
    fun `wpa2 to wpa returns 0_3`() {
        val input: ScanInput = scan(capabilities = "[WPA-PSK-TKIP]")
        val p: BssidProfile = profile(capabilitiesHistory = listOf("[WPA2-PSK-CCMP]"))
        val signal: ThreatSignal? = heuristic.evaluate(input, context(profiles = mapOf(input.bssid to p)))
        assertNotNull(signal)
        assertEquals(0.3f, signal!!.confidence, 0.001f)
    }

    @Test
    fun `owe to wpa2 same rank returns null`() {
        val input: ScanInput = scan(capabilities = "[WPA2-PSK-CCMP]")
        val p: BssidProfile = profile(capabilitiesHistory = listOf("[RSN-OWE-CCMP]"))
        val signal: ThreatSignal? = heuristic.evaluate(input, context(profiles = mapOf(input.bssid to p)))
        assertNull(signal)
    }

    @Test
    fun `wpa2 to wpa3 upgrade returns null`() {
        val input: ScanInput = scan(capabilities = "[RSN-SAE-CCMP]")
        val p: BssidProfile = profile(capabilitiesHistory = listOf("[WPA2-PSK-CCMP]"))
        val signal: ThreatSignal? = heuristic.evaluate(input, context(profiles = mapOf(input.bssid to p)))
        assertNull(signal)
    }

    @Test
    fun `no profile in context returns null`() {
        val input: ScanInput = scan()
        assertNull(heuristic.evaluate(input, context()))
    }

    @Test
    fun `profile with less than 3 observations returns null`() {
        val input: ScanInput = scan(capabilities = "[ESS]")
        val p: BssidProfile = profile(capabilitiesHistory = listOf("[WPA2-PSK-CCMP]"), observationCount = 2)
        val signal: ThreatSignal? = heuristic.evaluate(input, context(profiles = mapOf(input.bssid to p)))
        assertNull(signal)
    }

    @Test
    fun `profile spanning less than 2 days returns null`() {
        val input: ScanInput = scan(capabilities = "[ESS]")
        val p: BssidProfile = profile(capabilitiesHistory = listOf("[WPA2-PSK-CCMP]"), daySpan = 1)
        val signal: ThreatSignal? = heuristic.evaluate(input, context(profiles = mapOf(input.bssid to p)))
        assertNull(signal)
    }

    @Test
    fun `reason contains downgrade description`() {
        val input: ScanInput = scan(capabilities = "[ESS]")
        val p: BssidProfile = profile(capabilitiesHistory = listOf("[WPA2-PSK-CCMP]"))
        val signal: ThreatSignal? = heuristic.evaluate(input, context(profiles = mapOf(input.bssid to p)))
        assertNotNull(signal)
        assertTrue(signal!!.reasons[0].contains("downgrade"))
        assertTrue(signal.reasons[0].contains("WPA2"))
        assertTrue(signal.reasons[0].contains("OPEN"))
    }

    @Test
    fun `signal has correct source and type`() {
        val input: ScanInput = scan(capabilities = "[ESS]")
        val p: BssidProfile = profile(capabilitiesHistory = listOf("[WPA2-PSK-CCMP]"))
        val signal: ThreatSignal? = heuristic.evaluate(input, context(profiles = mapOf(input.bssid to p)))
        assertNotNull(signal)
        assertEquals(ThreatSource.LOCAL_HEURISTIC, signal!!.source)
        assertEquals(HeuristicType.ENCRYPTION_DOWNGRADE, signal.heuristicType)
    }
}
