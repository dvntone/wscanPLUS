package com.wscanplus.core.detector

import com.wscanplus.core.baseline.BaselineProfile
import com.wscanplus.core.baseline.TrustedApProfile
import com.wscanplus.core.evidence.ObservationEvent
import com.wscanplus.core.evidence.ObservationSource
import com.wscanplus.core.evidence.TimeBasis
import com.wscanplus.core.evidence.WifiObservationEvidence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrustedNetworkDriftDetectorTest {
    private val detector = TrustedNetworkDriftDetector()

    @Test
    fun `returns null for known access point within baseline`() {
        val result = detector.evaluate(observation(), baseline())

        assertNull(result)
    }

    @Test
    fun `detects known SSID with unknown BSSID`() {
        val result =
            detector.evaluate(
                observation(bssid = "11:22:33:44:55:66"),
                baseline(),
            )

        assertNotNull(result)
        assertEquals(listOf("Known SSID observed with untrusted BSSID"), result!!.reasons)
        assertEquals(0.65f, result.confidence)
        assertAndroidOnlyLimitations(result)
    }

    @Test
    fun `detects trusted BSSID on unexpected frequency`() {
        val result =
            detector.evaluate(
                observation(frequencyMhz = 2462),
                baseline(),
            )

        assertNotNull(result)
        assertEquals(listOf("Trusted BSSID observed on unexpected frequency"), result!!.reasons)
        assertEquals(0.45f, result.confidence)
    }

    @Test
    fun `detects trusted BSSID capability drift`() {
        val result =
            detector.evaluate(
                observation(capabilities = "[WPA-PSK-TKIP][ESS]"),
                baseline(),
            )

        assertNotNull(result)
        assertEquals(listOf("Trusted BSSID capabilities changed from baseline"), result!!.reasons)
        assertEquals(0.45f, result.confidence)
    }

    @Test
    fun `detects trusted BSSID RSSI outside baseline tolerance`() {
        val result =
            detector.evaluate(
                observation(rssiDbm = -75),
                baseline(),
            )

        assertNotNull(result)
        assertEquals(listOf("Trusted BSSID RSSI outside baseline tolerance"), result!!.reasons)
        assertEquals(0.45f, result.confidence)
    }

    @Test
    fun `combines multiple trusted BSSID drift reasons without exceeding confidence cap`() {
        val result =
            detector.evaluate(
                observation(
                    frequencyMhz = 2462,
                    capabilities = "[WPA-PSK-TKIP][ESS]",
                    rssiDbm = -75,
                ),
                baseline(),
            )

        assertNotNull(result)
        assertEquals(
            listOf(
                "Trusted BSSID observed on unexpected frequency",
                "Trusted BSSID capabilities changed from baseline",
                "Trusted BSSID RSSI outside baseline tolerance",
            ),
            result!!.reasons,
        )
        assertTrue(result.confidence <= 0.75f)
    }

    @Test
    fun `ignores observations for SSIDs not present in baseline`() {
        val result =
            detector.evaluate(
                observation(ssid = "CoffeeShop"),
                baseline(),
            )

        assertNull(result)
    }

    @Test
    fun `does not use confirmed attack or attribution wording`() {
        val result =
            detector.evaluate(
                observation(bssid = "11:22:33:44:55:66"),
                baseline(),
            )

        assertNotNull(result)
        val text = (result!!.reasons + result.limitations).joinToString(" ").lowercase()
        assertTrue("should not claim confirmed evil twin", "confirmed evil twin" !in text)
        assertTrue("should not claim attacker identity", "attacker" !in text)
    }

    private fun assertAndroidOnlyLimitations(result: TrustedNetworkDriftResult) {
        assertTrue(
            result.limitations.any { it.contains("scan snapshots") },
        )
        assertTrue(
            result.limitations.any { it.contains("does not confirm") },
        )
    }

    private fun baseline(): BaselineProfile =
        BaselineProfile(
            id = "home",
            zoneLabel = "Home",
            trustedAccessPoints =
                listOf(
                    TrustedApProfile(
                        ssid = "HomeNet",
                        bssid = "AA:BB:CC:DD:EE:FF",
                        expectedFrequenciesMhz = setOf(2412),
                        expectedCapabilities = setOf("[WPA2-PSK-CCMP][ESS]"),
                        rssiMedianDbm = -45,
                        rssiToleranceDb = 15,
                    ),
                ),
        )

    private fun observation(
        ssid: String = "HomeNet",
        bssid: String = "AA:BB:CC:DD:EE:FF",
        frequencyMhz: Int = 2412,
        capabilities: String = "[WPA2-PSK-CCMP][ESS]",
        rssiDbm: Int = -45,
    ): ObservationEvent =
        ObservationEvent(
            id = "obs-1",
            source = ObservationSource.ANDROID_WIFI,
            observedAtMs = 1_000L,
            observedAtBasis = TimeBasis.ELAPSED_REALTIME,
            wifi =
                WifiObservationEvidence(
                    ssid = ssid,
                    bssid = bssid,
                    hiddenSsid = ssid.isBlank(),
                    rssiDbm = rssiDbm,
                    frequencyMhz = frequencyMhz,
                    channelWidth = 0,
                    capabilities = capabilities,
                ),
        )
}
