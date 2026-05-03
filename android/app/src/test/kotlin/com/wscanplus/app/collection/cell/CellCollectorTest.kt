package com.wscanplus.app.collection.cell

import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CellCollectorTest {
    // 1. Fine location alone is sufficient below API 29
    @Test
    fun `hasReadPhoneStateRequired returns true when hasFine and sdkInt below Q`() {
        assertTrue(
            CellCollector.hasReadPhoneStateRequiredForCellInfo(
                sdkInt = Build.VERSION_CODES.P,
                hasFine = true,
                hasReadPhoneState = false,
            ),
        )
    }

    // 2. READ_PHONE_STATE required on API 29+
    @Test
    fun `hasReadPhoneStateRequired returns false on Q plus without READ_PHONE_STATE`() {
        assertFalse(
            CellCollector.hasReadPhoneStateRequiredForCellInfo(
                sdkInt = Build.VERSION_CODES.Q,
                hasFine = true,
                hasReadPhoneState = false,
            ),
        )
    }

    @Test
    fun `hasReadPhoneStateRequired returns true on Q plus with READ_PHONE_STATE`() {
        assertTrue(
            CellCollector.hasReadPhoneStateRequiredForCellInfo(
                sdkInt = Build.VERSION_CODES.Q,
                hasFine = true,
                hasReadPhoneState = true,
            ),
        )
    }

    // 3. Missing fine location → false regardless of API level
    @Test
    fun `hasReadPhoneStateRequired returns false when fine location missing on pre-Q`() {
        assertFalse(
            CellCollector.hasReadPhoneStateRequiredForCellInfo(
                sdkInt = Build.VERSION_CODES.P,
                hasFine = false,
                hasReadPhoneState = true,
            ),
        )
    }

    @Test
    fun `hasReadPhoneStateRequired returns false when fine location missing on Q plus`() {
        assertFalse(
            CellCollector.hasReadPhoneStateRequiredForCellInfo(
                sdkInt = Build.VERSION_CODES.Q,
                hasFine = false,
                hasReadPhoneState = true,
            ),
        )
    }

    // 4. CellObservation data class holds all fields (ci is Long for NR compatibility)
    @Test
    fun `CellObservation holds all fields correctly`() {
        val obs =
            CellObservation(
                mcc = 310,
                mnc = 260,
                tac = 1234,
                ci = 56789L,
                pci = 42,
                type = CellType.LTE,
                rsrp = -90,
                rsrq = -10,
                sinr = 5,
                observedAtMs = 1_700_000_000_000L,
            )

        assertEquals(310, obs.mcc)
        assertEquals(260, obs.mnc)
        assertEquals(1234, obs.tac)
        assertEquals(56789L, obs.ci)
        assertEquals(42, obs.pci)
        assertEquals(CellType.LTE, obs.type)
        assertEquals(-90, obs.rsrp)
        assertEquals(-10, obs.rsrq)
        assertEquals(5, obs.sinr)
        assertEquals(1_700_000_000_000L, obs.observedAtMs)
    }

    // 5. Null fields stay null (sentinel contract — Int.MAX_VALUE and Long.MAX_VALUE map to null)
    @Test
    fun `CellObservation accepts all null optional fields`() {
        val obs =
            CellObservation(
                mcc = null,
                mnc = null,
                tac = null,
                ci = null,
                pci = null,
                type = CellType.UNKNOWN,
                rsrp = null,
                rsrq = null,
                sinr = null,
                observedAtMs = 0L,
            )

        assertNull(obs.mcc)
        assertNull(obs.mnc)
        assertNull(obs.tac)
        assertNull(obs.ci)
        assertNull(obs.pci)
        assertNull(obs.rsrp)
        assertNull(obs.rsrq)
        assertNull(obs.sinr)
    }
}
