package com.wscanplus.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WatchdogServiceBssidTest {
    @Test
    fun `typical consumer AP BSSID maps to gateway IP`() {
        assertEquals("212.106.53.1", WatchdogService.bssidToGatewayIp("D4:6A:35:AB:CD:EF"))
    }

    @Test
    fun `lowercase bssid is handled`() {
        assertEquals("212.106.53.1", WatchdogService.bssidToGatewayIp("d4:6a:35:ab:cd:ef"))
    }

    @Test
    fun `multicast prefix returns null`() {
        assertNull(WatchdogService.bssidToGatewayIp("E0:AB:CD:01:02:03"))
    }

    @Test
    fun `all-zero first octet returns null`() {
        assertNull(WatchdogService.bssidToGatewayIp("00:AB:CD:01:02:03"))
    }

    @Test
    fun `malformed bssid returns null`() {
        assertNull(WatchdogService.bssidToGatewayIp("not-a-bssid"))
    }

    @Test
    fun `too few octets returns null`() {
        assertNull(WatchdogService.bssidToGatewayIp("AA:BB:CC"))
    }

    @Test
    fun `broadcast bssid returns null`() {
        assertNull(WatchdogService.bssidToGatewayIp("FF:FF:FF:FF:FF:FF"))
    }
}
