package com.wscanplus.core.evidence

import org.junit.Assert.assertEquals
import org.junit.Test

class EvidenceProvenanceTest {
    @Test
    fun preserves_valid_provenance_fields() {
        val provenance =
            EvidenceProvenance(
                sourceType = EvidenceSourceType.ANDROID_WIFI_SCAN,
                collectorId = "collector-a",
                ingestionPath = "android/wifi",
                observationTimestampMs = 1000L,
                receivedTimestampMs = 1200L,
            )

        assertEquals(EvidenceSourceType.ANDROID_WIFI_SCAN, provenance.sourceType)
        assertEquals("collector-a", provenance.collectorId)
        assertEquals("android/wifi", provenance.ingestionPath)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejects_blank_ingestion_path() {
        EvidenceProvenance(
            sourceType = EvidenceSourceType.DESKTOP_IMPORT,
            collectorId = null,
            ingestionPath = " ",
            observationTimestampMs = 1000L,
            receivedTimestampMs = 1200L,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejects_negative_observation_timestamp() {
        EvidenceProvenance(
            sourceType = EvidenceSourceType.BLE_TETHER,
            collectorId = null,
            ingestionPath = "ble/tether",
            observationTimestampMs = -1L,
            receivedTimestampMs = 1200L,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejects_negative_received_timestamp() {
        EvidenceProvenance(
            sourceType = EvidenceSourceType.USB_TETHER,
            collectorId = null,
            ingestionPath = "usb/tether",
            observationTimestampMs = 1000L,
            receivedTimestampMs = -1L,
        )
    }
}
