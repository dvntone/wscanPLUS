package com.wscanplus.core.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class WifiScanResultTest {
    private val result =
        WifiScanResult(
            ssid = "TestNetwork",
            bssid = "AA:BB:CC:DD:EE:FF",
            signalLevel = -65,
            frequencyMhz = 5180,
            capabilities = "[WPA2-PSK-CCMP][ESS]",
            timestamp = 1234567890L,
            channelWidth = 1,
            centerFreq0 = 5190,
            centerFreq1 = 0,
        )

    @Test
    fun `data class equality works on matching fields`() {
        val copy = result.copy()
        assertEquals(result, copy)
        assertEquals(result.hashCode(), copy.hashCode())
    }

    @Test
    fun `data class inequality on different BSSID`() {
        val different = result.copy(bssid = "11:22:33:44:55:66")
        assertNotEquals(result, different)
    }

    @Test
    fun `fields are accessible and match constructor values`() {
        assertEquals("TestNetwork", result.ssid)
        assertEquals("AA:BB:CC:DD:EE:FF", result.bssid)
        assertEquals(-65, result.signalLevel)
        assertEquals(5180, result.frequencyMhz)
        assertEquals("[WPA2-PSK-CCMP][ESS]", result.capabilities)
        assertEquals(1234567890L, result.timestamp)
        assertEquals(1, result.channelWidth)
        assertEquals(5190, result.centerFreq0)
        assertEquals(0, result.centerFreq1)
    }

    @Test
    fun `empty SSID is valid (hidden network)`() {
        val hidden = result.copy(ssid = "")
        assertEquals("", hidden.ssid)
    }

    @Test
    fun `negative signal level represents weak signal`() {
        val weak = result.copy(signalLevel = -90)
        assertEquals(-90, weak.signalLevel)
    }

    @Test
    fun `2_4GHz frequency range`() {
        val result24 = result.copy(frequencyMhz = 2437)
        assertEquals(2437, result24.frequencyMhz)
    }
}
