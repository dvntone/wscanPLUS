package com.wscanplus.core.threat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OuiLookupTest {
    private val lookup =
        OuiLookup(
            mapOf(
                "B827EB" to "Raspberry Pi Trading Ltd",
                "240AC4" to "Espressif Inc.",
            ),
        )

    @Test
    fun `lookup returns vendor for known OUI`() {
        val vendor = lookup.lookup("B8:27:EB:11:22:33")
        assertEquals("Raspberry Pi Trading Ltd", vendor)
    }

    @Test
    fun `lookup returns null for unknown OUI`() {
        val vendor = lookup.lookup("AA:BB:CC:11:22:33")
        assertNull(vendor)
    }

    @Test
    fun `suspicious vendor detected`() {
        assertTrue(lookup.isSuspiciousVendor("B8:27:EB:11:22:33"))
    }

    @Test
    fun `non suspicious vendor returns false`() {
        assertFalse(lookup.isSuspiciousVendor("AA:BB:CC:11:22:33"))
    }

    @Test
    fun `locally administered mac detected`() {
        assertTrue(lookup.isLocallyAdministered("02:11:22:33:44:55"))
    }

    @Test
    fun `globally administered mac returns false`() {
        assertFalse(lookup.isLocallyAdministered("00:11:22:33:44:55"))
    }

    @Test
    fun `malformed mac returns false`() {
        assertFalse(lookup.isLocallyAdministered("ZZ"))
    }

    @Test
    fun `lowercase bssid works`() {
        val vendor = lookup.lookup("b8:27:eb:aa:bb:cc")
        assertEquals("Raspberry Pi Trading Ltd", vendor)
    }
}
