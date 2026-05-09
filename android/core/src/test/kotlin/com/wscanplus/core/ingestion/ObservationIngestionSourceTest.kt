package com.wscanplus.core.ingestion

import com.wscanplus.core.evidence.EvidenceSourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ObservationIngestionSourceTest {
    @Test
    fun android_source_preserves_transport_metadata() {
        val source =
            AndroidObservationSource(
                sourceId = "android-primary",
                sourceType = EvidenceSourceType.ANDROID_WIFI_SCAN,
                displayName = "Primary Android Scanner",
            )

        assertEquals(ObservationTransport.LOCAL_ANDROID, source.transport)
        assertEquals(EvidenceSourceType.ANDROID_WIFI_SCAN, source.sourceType)
        assertFalse(source.isUserInitiated)
    }

    @Test
    fun desktop_import_is_user_initiated() {
        val source =
            DesktopImportSource(
                sourceId = "desktop-import",
                displayName = "Desktop Import",
            )

        assertEquals(ObservationTransport.DESKTOP_IMPORT, source.transport)
        assertEquals(EvidenceSourceType.DESKTOP_IMPORT, source.sourceType)
        assertTrue(source.isUserInitiated)
    }

    @Test
    fun usb_tether_source_uses_usb_transport() {
        val source =
            UsbTetherSource(
                sourceId = "usb-source",
                displayName = "USB Companion",
            )

        assertEquals(ObservationTransport.USB_TETHER, source.transport)
        assertEquals(EvidenceSourceType.USB_TETHER, source.sourceType)
    }

    @Test
    fun ble_tether_source_uses_ble_transport() {
        val source =
            BleTetherSource(
                sourceId = "ble-source",
                displayName = "BLE Companion",
            )

        assertEquals(ObservationTransport.BLE_TETHER, source.transport)
        assertEquals(EvidenceSourceType.BLE_TETHER, source.sourceType)
    }
}
