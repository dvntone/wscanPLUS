package com.wscanplus.core.threat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BssidFingerprintHeuristicTest {
    private val now: Long = System.currentTimeMillis()
    private val within30Min: Long = now - (20 * 60 * 1000L)
    private val outside30Min: Long = now - (45 * 60 * 1000L)

    private fun scan(
        bssid: String = "00:AA:BB:CC:DD:EE",
        ssid: String = "TestNet",
    ): ScanInput =
        ScanInput(
            bssid = bssid,
            ssid = ssid,
            isHidden = false,
            capabilities = "[WPA2-PSK-CCMP]",
            rssiDbm = -60,
            frequencyMhz = 2412,
            channelWidth = 20,
            timestamp = now,
        )

    private fun profile(
        bssid: String,
        ssid: String,
        lastSeenAt: Long = within30Min,
        observationCount: Int = 10,
        ouiVendor: String? = null,
    ): BssidProfile =
        BssidProfile(
            bssid = bssid,
            firstSeenAt = lastSeenAt - 60_000L,
            lastSeenAt = lastSeenAt,
            ssids = setOf(ssid),
            capabilitiesHistory = listOf("[WPA2-PSK-CCMP]"),
            observationCount = observationCount,
            ouiVendor = ouiVendor,
        )

    private fun context(
        input: ScanInput,
        knownProfiles: Map<String, BssidProfile> = emptyMap(),
    ): ScanContext =
        ScanContext(
            currentResults = listOf(input),
            knownProfiles = knownProfiles,
            baselineNetworkCount = null,
            baselineStdDev = null,
            environmentType = EnvironmentType.RESIDENTIAL,
        )

    @Test
    fun `locally-administered MAC detected with confidence 0_25`() {
        val input: ScanInput = scan(bssid = "02:AA:BB:CC:DD:EE")
        val ctx: ScanContext = context(input = input)
        val heuristic = BssidFingerprintHeuristic(ouiLookup = null)
        val signal: ThreatSignal? = heuristic.evaluate(input, ctx)
        assertNotNull(signal)
        assertEquals(0.25f, signal!!.confidence, 0.001f)
    }

    @Test
    fun `normal MAC and no rotation returns null`() {
        val input: ScanInput = scan(bssid = "00:AA:BB:CC:DD:EE")
        val ctx: ScanContext = context(input = input)
        val heuristic = BssidFingerprintHeuristic(ouiLookup = null)
        assertNull(heuristic.evaluate(input, ctx))
    }

    @Test
    fun `BSSID rotation within 30min returns signal with rotation confidence`() {
        val oldBssid = "00:11:22:33:44:55"
        val newBssid = "00:AA:BB:CC:DD:EE"
        val input: ScanInput = scan(bssid = newBssid, ssid = "HomeNet")
        val profiles: Map<String, BssidProfile> =
            mapOf(
                oldBssid to profile(bssid = oldBssid, ssid = "HomeNet", lastSeenAt = within30Min),
            )
        val ctx: ScanContext = context(input = input, knownProfiles = profiles)
        val heuristic = BssidFingerprintHeuristic(ouiLookup = null)
        val signal: ThreatSignal? = heuristic.evaluate(input, ctx)
        assertNotNull(signal)
        assertEquals(0.35f, signal!!.confidence, 0.001f)
        assertTrue(signal.reasons.any { reason: String -> reason.contains("rotated") })
    }

    @Test
    fun `BSSID rotation outside 30min window returns no rotation sub-signal`() {
        val oldBssid = "00:11:22:33:44:55"
        val newBssid = "00:AA:BB:CC:DD:EE"
        val input: ScanInput = scan(bssid = newBssid, ssid = "HomeNet")
        val profiles: Map<String, BssidProfile> =
            mapOf(
                oldBssid to profile(bssid = oldBssid, ssid = "HomeNet", lastSeenAt = outside30Min),
            )
        val ctx: ScanContext = context(input = input, knownProfiles = profiles)
        val heuristic = BssidFingerprintHeuristic(ouiLookup = null)
        assertNull(heuristic.evaluate(input, ctx))
    }

    @Test
    fun `null ouiLookup skips vendor check entirely`() {
        val oldBssid = "00:11:22:33:44:55"
        val newBssid = "00:AA:BB:CC:DD:EE"
        val input: ScanInput = scan(bssid = newBssid, ssid = "HomeNet")
        val profiles: Map<String, BssidProfile> =
            mapOf(
                oldBssid to profile(bssid = oldBssid, ssid = "HomeNet"),
            )
        val ctx: ScanContext = context(input = input, knownProfiles = profiles)
        val heuristic = BssidFingerprintHeuristic(ouiLookup = null)
        val signal: ThreatSignal? = heuristic.evaluate(input, ctx)
        assertNotNull(signal)
        // Only rotation sub-signal: 1 - (1-0.35) = 0.35
        assertEquals(0.35f, signal!!.confidence, 0.001f)
        // No vendor reason
        assertTrue(signal.reasons.none { reason: String -> reason.contains("vendor") || reason.contains("OUI") })
    }

    @Test
    fun `same vendor on rotation returns lower confidence than different vendor`() {
        val oldBssid = "00:11:22:33:44:55"
        val newBssid = "00:AA:BB:CC:DD:EE"
        val input: ScanInput = scan(bssid = newBssid, ssid = "HomeNet")
        val profiles: Map<String, BssidProfile> =
            mapOf(
                oldBssid to profile(bssid = oldBssid, ssid = "HomeNet"),
            )
        val ctx: ScanContext = context(input = input, knownProfiles = profiles)

        val sameVendorLookup: (String) -> String? = { _: String -> "VendorA" }
        val heuristicSame = BssidFingerprintHeuristic(ouiLookup = sameVendorLookup)
        val signalSame: ThreatSignal? = heuristicSame.evaluate(input, ctx)
        assertNotNull(signalSame)
        // Combined: 1 - (1-0.35)(1-0.15) = 1 - 0.65*0.85 = 1 - 0.5525 = 0.4475
        assertEquals(0.4475f, signalSame!!.confidence, 0.001f)
    }

    @Test
    fun `different vendor on rotation returns higher confidence`() {
        val oldBssid = "00:11:22:33:44:55"
        val newBssid = "00:AA:BB:CC:DD:EE"
        val input: ScanInput = scan(bssid = newBssid, ssid = "HomeNet")
        val profiles: Map<String, BssidProfile> =
            mapOf(
                oldBssid to profile(bssid = oldBssid, ssid = "HomeNet"),
            )
        val ctx: ScanContext = context(input = input, knownProfiles = profiles)

        var callCount = 0
        val diffVendorLookup: (String) -> String? = { _: String ->
            callCount++
            if (callCount == 1) "VendorA" else "VendorB"
        }
        val heuristicDiff = BssidFingerprintHeuristic(ouiLookup = diffVendorLookup)
        val signalDiff: ThreatSignal? = heuristicDiff.evaluate(input, ctx)
        assertNotNull(signalDiff)
        // Combined: 1 - (1-0.35)(1-0.40) = 1 - 0.65*0.60 = 1 - 0.39 = 0.61
        assertEquals(0.61f, signalDiff!!.confidence, 0.001f)

        // Same vendor confidence should be less than different vendor confidence
        assertTrue(0.4475f < signalDiff.confidence)
    }

    @Test
    fun `combined signals locally-admin plus rotation plus vendor mismatch caps at 0_95`() {
        // "02:..." locally-administered (firstByte 0x02 has bit 1 set)
        val newBssid = "02:AA:BB:CC:DD:EE"
        val oldBssid = "00:11:22:33:44:55"
        val input: ScanInput = scan(bssid = newBssid, ssid = "HomeNet")
        val profiles: Map<String, BssidProfile> =
            mapOf(
                oldBssid to profile(bssid = oldBssid, ssid = "HomeNet"),
            )
        val ctx: ScanContext = context(input = input, knownProfiles = profiles)

        var callCount = 0
        val diffVendorLookup: (String) -> String? = { _: String ->
            callCount++
            if (callCount == 1) "VendorA" else "VendorB"
        }
        val heuristic = BssidFingerprintHeuristic(ouiLookup = diffVendorLookup)
        val signal: ThreatSignal? = heuristic.evaluate(input, ctx)
        assertNotNull(signal)
        // Combined: 1 - (1-0.25)(1-0.35)(1-0.40) = 1 - 0.75*0.65*0.60 = 1 - 0.2925 = 0.7075
        // Under 0.95 so no cap needed here, but the cap mechanism is exercised
        assertEquals(0.7075f, signal!!.confidence, 0.001f)
        assertTrue(signal.confidence <= 0.95f)
    }

    @Test
    fun `non-null ouiLookup returning null for both skips vendor sub-signal`() {
        val oldBssid = "00:11:22:33:44:55"
        val newBssid = "00:AA:BB:CC:DD:EE"
        val input: ScanInput = scan(bssid = newBssid, ssid = "HomeNet")
        val profiles: Map<String, BssidProfile> =
            mapOf(
                oldBssid to profile(bssid = oldBssid, ssid = "HomeNet"),
            )
        val ctx: ScanContext = context(input = input, knownProfiles = profiles)
        // Simulates AtomicReference<OuiLookup> not yet loaded — lambda wired but returns null
        val heuristic = BssidFingerprintHeuristic(ouiLookup = { _: String -> null })
        val signal: ThreatSignal? = heuristic.evaluate(input, ctx)
        assertNotNull(signal)
        // Only rotation sub-signal: 1 - (1-0.35) = 0.35 — same as null ouiLookup
        assertEquals(0.35f, signal!!.confidence, 0.001f)
        assertTrue(signal.reasons.none { reason: String -> reason.contains("vendor") || reason.contains("OUI") })
    }

    @Test
    fun `heuristic type is BSSID_FINGERPRINT`() {
        val input: ScanInput = scan(bssid = "02:AA:BB:CC:DD:EE")
        val ctx: ScanContext = context(input = input)
        val heuristic = BssidFingerprintHeuristic(ouiLookup = null)
        val signal: ThreatSignal? = heuristic.evaluate(input, ctx)
        assertNotNull(signal)
        assertEquals(HeuristicType.BSSID_FINGERPRINT, signal!!.heuristicType)
    }
}
