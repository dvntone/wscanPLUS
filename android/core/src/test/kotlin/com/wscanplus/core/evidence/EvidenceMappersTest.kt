package com.wscanplus.core.evidence

import com.wscanplus.core.scanner.WifiScanResult
import com.wscanplus.core.threat.HeuristicType
import com.wscanplus.core.threat.ScanInput
import com.wscanplus.core.threat.ThreatSignal
import com.wscanplus.core.threat.ThreatSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EvidenceMappersTest {
    @Test
    fun `WifiScanResult maps factual fields into observation event`() {
        val result =
            WifiScanResult(
                ssid = "LabNet",
                bssid = "AA:BB:CC:DD:EE:FF",
                signalLevel = -42,
                frequencyMhz = 2412,
                capabilities = "[WPA2-PSK-CCMP][ESS]",
                timestamp = 123_456_000L,
                channelWidth = 0,
                centerFreq0 = 0,
                centerFreq1 = 0,
            )

        val event = result.toObservationEvent(id = "obs-1")

        assertEquals("obs-1", event.id)
        assertEquals(ObservationSource.ANDROID_WIFI, event.source)
        assertEquals("android_wifi", event.source.contractValue)
        assertEquals(123_456L, event.observedAtMs)
        assertEquals(TimeBasis.ELAPSED_REALTIME, event.observedAtBasis)
        assertEquals("LabNet", event.wifi.ssid)
        assertEquals("AA:BB:CC:DD:EE:FF", event.wifi.bssid)
        assertFalse(event.wifi.hiddenSsid)
        assertEquals(-42, event.wifi.rssiDbm)
        assertEquals(2412, event.wifi.frequencyMhz)
        assertEquals(0, event.wifi.channelWidth)
        assertEquals("[WPA2-PSK-CCMP][ESS]", event.wifi.capabilities)
    }

    @Test
    fun `WifiScanResult default ID uses same millisecond timestamp as observation`() {
        val result =
            WifiScanResult(
                ssid = "LabNet",
                bssid = "AA:BB:CC:DD:EE:FF",
                signalLevel = -42,
                frequencyMhz = 2412,
                capabilities = "[WPA2-PSK-CCMP][ESS]",
                timestamp = 123_456_000L,
                channelWidth = 0,
                centerFreq0 = 0,
                centerFreq1 = 0,
            )

        val event = result.toObservationEvent()

        assertEquals(123_456L, event.observedAtMs)
        assertEquals(TimeBasis.ELAPSED_REALTIME, event.observedAtBasis)
        assertEquals("android-wifi:aa:bb:cc:dd:ee:ff:123456", event.id)
    }

    @Test
    fun `hidden SSID remains an observed fact only`() {
        val input =
            ScanInput(
                bssid = "11:22:33:44:55:66",
                ssid = "",
                isHidden = true,
                capabilities = "[ESS]",
                rssiDbm = -65,
                frequencyMhz = 5180,
                channelWidth = 1,
                timestamp = 321L,
            )

        val event = input.toObservationEvent(id = "obs-hidden")

        assertEquals("", event.wifi.ssid)
        assertTrue(event.wifi.hiddenSsid)
        assertEquals(ObservationSource.ANDROID_WIFI, event.source)
        assertEquals(TimeBasis.EPOCH, event.observedAtBasis)
    }

    @Test
    fun `ThreatSignal maps to detection evidence without changing confidence or reasons`() {
        val observation =
            ScanInput(
                bssid = "AA:BB:CC:DD:EE:FF",
                ssid = "LabNet",
                isHidden = false,
                capabilities = "[WPA2-PSK-CCMP][ESS]",
                rssiDbm = -31,
                frequencyMhz = 2412,
                channelWidth = 0,
                timestamp = 1_000L,
            ).toObservationEvent(id = "obs-1")

        val signal =
            ThreatSignal(
                confidence = 0.45f,
                source = ThreatSource.LOCAL_HEURISTIC,
                reasons = listOf("Observed RSSI anomaly"),
                heuristicType = HeuristicType.RSSI_ANOMALY,
                bssid = "AA:BB:CC:DD:EE:FF",
                detectedAt = 2_000L,
            )

        val evidence = signal.toDetectionEvidenceEvent(observation = observation, id = "ev-1")

        assertEquals("ev-1", evidence.id)
        assertEquals("obs-1", evidence.observationId)
        assertEquals(ThreatSource.LOCAL_HEURISTIC, evidence.source)
        assertEquals(HeuristicType.RSSI_ANOMALY, evidence.heuristicType)
        assertEquals(0.45f, evidence.confidence)
        assertEquals(listOf("Observed RSSI anomaly"), evidence.reasons)
        assertEquals("AA:BB:CC:DD:EE:FF", evidence.bssid)
        assertEquals(2_000L, evidence.detectedAtMs)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `DetectionEvidenceEvent enforces existing confidence cap`() {
        DetectionEvidenceEvent(
            id = "bad",
            observationId = "obs",
            source = ThreatSource.LOCAL_HEURISTIC,
            heuristicType = HeuristicType.EVIL_TWIN,
            confidence = 0.96f,
            reasons = listOf("too high"),
            bssid = "AA:BB:CC:DD:EE:FF",
            detectedAtMs = 1L,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `DetectionEvidenceEvent enforces reason count cap`() {
        DetectionEvidenceEvent(
            id = "bad-reasons",
            observationId = "obs",
            source = ThreatSource.LOCAL_HEURISTIC,
            heuristicType = HeuristicType.EVIL_TWIN,
            confidence = 0.45f,
            reasons = listOf("one", "two", "three", "four"),
            bssid = "AA:BB:CC:DD:EE:FF",
            detectedAtMs = 1L,
        )
    }
}
