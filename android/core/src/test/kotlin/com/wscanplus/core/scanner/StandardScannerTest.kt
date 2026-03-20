package com.wscanplus.core.scanner

import android.os.Build
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StandardScannerTest {
    @Test
    fun `legacy scan request remains enabled on android 9 and 10 path`() {
        assertTrue(StandardScanner.shouldRequestLegacyScan(Build.VERSION_CODES.P))
        assertTrue(StandardScanner.shouldRequestLegacyScan(Build.VERSION_CODES.Q))
    }

    @Test
    fun `legacy scan request disabled once scan results callback path is available`() {
        assertFalse(StandardScanner.shouldRequestLegacyScan(Build.VERSION_CODES.R))
        assertFalse(StandardScanner.shouldRequestLegacyScan(Build.VERSION_CODES.TIRAMISU))
    }
}
