package com.wscanplus.core.threat

import org.junit.Assert.assertEquals
import org.junit.Test

class SecurityTypeTest {
    @Test
    fun `parse WPA2 capabilities`() {
        val result: SecurityType = SecurityType.parse("[WPA2-PSK-CCMP][RSN-PSK-CCMP][ESS]")
        assertEquals(SecurityType.WPA2, result)
    }

    @Test
    fun `parse WPA3 SAE takes precedence`() {
        val result: SecurityType =
            SecurityType.parse("[WPA3-SAE+SAE-EXT-KEY][RSN-SAE+SAE-EXT-KEY-CCMP]")
        assertEquals(SecurityType.WPA3, result)
    }

    @Test
    fun `parse OWE Enhanced Open`() {
        val result: SecurityType = SecurityType.parse("[RSN-OWE-CCMP]")
        assertEquals(SecurityType.OWE, result)
    }

    @Test
    fun `parse WPA original`() {
        val result: SecurityType = SecurityType.parse("[WPA-PSK-TKIP]")
        assertEquals(SecurityType.WPA, result)
    }

    @Test
    fun `parse WEP`() {
        val result: SecurityType = SecurityType.parse("[WEP]")
        assertEquals(SecurityType.WEP, result)
    }

    @Test
    fun `parse empty string returns OPEN`() {
        val result: SecurityType = SecurityType.parse("")
        assertEquals(SecurityType.OPEN, result)
    }

    @Test
    fun `parse ESS only returns OPEN`() {
        val result: SecurityType = SecurityType.parse("[ESS]")
        assertEquals(SecurityType.OPEN, result)
    }

    @Test
    fun `parse case insensitive`() {
        val result: SecurityType = SecurityType.parse("[wpa2-psk-ccmp]")
        assertEquals(SecurityType.WPA2, result)
    }

    @Test
    fun `parse SAE wins over WPA in mixed string`() {
        val result: SecurityType = SecurityType.parse("[WPA-PSK][SAE]")
        assertEquals(SecurityType.WPA3, result)
    }
}
