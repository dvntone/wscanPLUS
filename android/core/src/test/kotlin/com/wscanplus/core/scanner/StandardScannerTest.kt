package com.wscanplus.core.scanner

import android.os.Build
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StandardScannerTest {
    @Test
    fun `legacy broadcast path remains enabled on android 9 and 10`() {
        assertTrue(StandardScanner.usesLegacyBroadcastPath(Build.VERSION_CODES.P))
        assertTrue(StandardScanner.usesLegacyBroadcastPath(Build.VERSION_CODES.Q))
    }

    @Test
    fun `legacy broadcast path disabled once scan results callback path is available`() {
        assertFalse(StandardScanner.usesLegacyBroadcastPath(Build.VERSION_CODES.R))
        assertFalse(StandardScanner.usesLegacyBroadcastPath(Build.VERSION_CODES.TIRAMISU))
    }
}
