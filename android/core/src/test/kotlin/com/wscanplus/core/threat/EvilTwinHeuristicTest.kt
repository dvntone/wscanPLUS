package com.wscanplus.core.threat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EvilTwinHeuristicTest {
    private val heuristic = EvilTwinHeuristic()

    private fun scan(
        bssid: String = "AA:BB:CC:DD:EE:FF",
        ssid: String = "TestNet",
        capabilities: String = "[WPA2-PSK-CCMP][RSN-PSK-CCMP]",
        rssiDbm: Int = -50,
        frequencyMhz: Int = 2412,
    ): ScanInput =
        ScanInput(
            bssid = bssid,
            ssid = ssid,
            isHidden = false,
            capabilities = capabilities,
            rssiDbm = rssiDbm,
            frequencyMhz = frequencyMhz,
            channelWidth = 20,
            timestamp = System.currentTimeMillis(),
        )

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

    private fun testProfile(bssid: String = "11:22:33:44:55:66"): BssidProfile =
        BssidProfile(
            bssid = bssid,
            firstSeenAt = 0L,
            lastSeenAt = 1L,
            ssids = setOf("TestNet"),
            capabilitiesHistory = listOf("[WPA2-PSK-CCMP]"),
            observationCount = 10,
            ouiVendor = "VendorB",
        )

    @Test
    fun `single network with no duplicate SSID returns null`() {
        val input: ScanInput = scan()
        val ctx: ScanContext = context(results = listOf(input))
        assertNull(heuristic.evaluate(input, ctx))
    }

    @Test
    fun `guest network with shared first 5 octets returns null`() {
        val input: ScanInput = scan(bssid = "AA:BB:CC:DD:EE:01")
        val peer: ScanInput = scan(bssid = "AA:BB:CC:DD:EE:02", capabilities = "[ESS]")
        val ctx: ScanContext = context(results = listOf(input, peer))
        assertNull(heuristic.evaluate(input, ctx))
    }

    @Test
    fun `capability mismatch alone fires with confidence 0_35`() {
        val input: ScanInput =
            scan(bssid = "AA:BB:CC:DD:EE:FF", capabilities = "[WPA2-PSK-CCMP]", frequencyMhz = 2412)
        val peer: ScanInput =
            scan(bssid = "11:22:33:44:55:66", capabilities = "[ESS]", frequencyMhz = 5180)
        val ctx: ScanContext = context(results = listOf(input, peer))
        val signal: ThreatSignal? = heuristic.evaluate(input, ctx)
        assertNotNull(signal)
        assertEquals(0.35f, signal!!.confidence, 0.001f)
    }

    @Test
    fun `oui plus capability mismatch combines to about 0_545`() {
        val lookup: (String) -> String? = { prefix: String ->
            when (prefix) {
                "AA:BB:CC" -> "VendorA"
                "11:22:33" -> "VendorB"
                else -> null
            }
        }
        val h = EvilTwinHeuristic(vendorLookup = lookup)
        val input: ScanInput =
            scan(bssid = "AA:BB:CC:DD:EE:FF", capabilities = "[WPA2-PSK-CCMP]", frequencyMhz = 2412)
        val peer: ScanInput =
            scan(bssid = "11:22:33:44:55:66", capabilities = "[ESS]", frequencyMhz = 5180)
        val ctx: ScanContext = context(results = listOf(input, peer))
        val signal: ThreatSignal? = h.evaluate(input, ctx)
        assertNotNull(signal)
        val expected: Float = 1f - (1f - 0.3f) * (1f - 0.35f)
        assertEquals(expected, signal!!.confidence, 0.001f)
    }

    @Test
    fun `null vendorLookup skips OUI sub-signal`() {
        val input: ScanInput = scan(bssid = "AA:BB:CC:DD:EE:FF", capabilities = "[WPA2-PSK-CCMP]")
        val peer: ScanInput =
            scan(bssid = "11:22:33:44:55:66", capabilities = "[WPA2-PSK-CCMP]", frequencyMhz = 5180)
        val ctx: ScanContext = context(results = listOf(input, peer))
        val signal: ThreatSignal? = heuristic.evaluate(input, ctx)
        assertNull(signal)
    }

    @Test
    fun `enterprise multi-AP same OUI still evaluates sub-signals`() {
        val input: ScanInput =
            scan(bssid = "AA:BB:CC:11:11:11", capabilities = "[WPA2-PSK-CCMP]", rssiDbm = -30)
        val peer: ScanInput =
            scan(bssid = "AA:BB:CC:22:22:22", capabilities = "[ESS]", rssiDbm = -60)
        val ctx: ScanContext = context(results = listOf(input, peer))
        val signal: ThreatSignal? = heuristic.evaluate(input, ctx)
        assertNotNull(signal)
        assertTrue(signal!!.confidence > 0f)
    }

    @Test
    fun `confidence capped at 0_95`() {
        val lookup: (String) -> String? = { prefix: String ->
            when (prefix) {
                "AA:BB:CC" -> "VendorA"
                "11:22:33" -> "VendorB"
                else -> null
            }
        }
        val h = EvilTwinHeuristic(vendorLookup = lookup)
        val profile: BssidProfile = testProfile()
        val input: ScanInput =
            scan(bssid = "AA:BB:CC:DD:EE:FF", capabilities = "[ESS]", rssiDbm = -20)
        val peer: ScanInput =
            scan(bssid = "11:22:33:44:55:66", capabilities = "[WPA2-PSK-CCMP]", rssiDbm = -70)
        val ctx: ScanContext =
            context(
                results = listOf(input, peer),
                profiles = mapOf("11:22:33:44:55:66" to profile),
            )
        val signal: ThreatSignal? = h.evaluate(input, ctx)
        assertNotNull(signal)
        assertTrue(signal!!.confidence <= 0.95f)
    }

    @Test
    fun `reasons list has max 3 entries`() {
        val lookup: (String) -> String? = { prefix: String ->
            when (prefix) {
                "AA:BB:CC" -> "VendorA"
                "11:22:33" -> "VendorB"
                else -> null
            }
        }
        val h = EvilTwinHeuristic(vendorLookup = lookup)
        val profile: BssidProfile = testProfile()
        val input: ScanInput =
            scan(bssid = "AA:BB:CC:DD:EE:FF", capabilities = "[ESS]", rssiDbm = -20)
        val peer: ScanInput =
            scan(bssid = "11:22:33:44:55:66", capabilities = "[WPA2-PSK-CCMP]", rssiDbm = -70)
        val ctx: ScanContext =
            context(
                results = listOf(input, peer),
                profiles = mapOf("11:22:33:44:55:66" to profile),
            )
        val signal: ThreatSignal? = h.evaluate(input, ctx)
        assertNotNull(signal)
        assertTrue(signal!!.reasons.size <= 3)
    }

    @Test
    fun `reasons contain descriptive text`() {
        val input: ScanInput = scan(bssid = "AA:BB:CC:DD:EE:FF", capabilities = "[WPA2-PSK-CCMP]")
        val peer: ScanInput = scan(bssid = "11:22:33:44:55:66", capabilities = "[ESS]")
        val ctx: ScanContext = context(results = listOf(input, peer))
        val signal: ThreatSignal? = heuristic.evaluate(input, ctx)
        assertNotNull(signal)
        assertTrue(signal!!.reasons.any { reason: String -> reason.contains("capability") })
    }
}
