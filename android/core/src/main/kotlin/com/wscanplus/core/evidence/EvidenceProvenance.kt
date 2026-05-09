package com.wscanplus.core.evidence

/**
 * Provenance metadata describing where evidence originated.
 *
 * Provenance is operational metadata only.
 * It does not imply attribution, device identity, or actor identification.
 *
 * Source provenance remains separate from detector confidence so downstream
 * aggregation can reason about observation origin independently from threat
 * assessment severity.
 */
data class EvidenceProvenance(
    val sourceType: EvidenceSourceType,
    val collectorId: String?,
    val ingestionPath: String,
    val observationTimestampMs: Long,
    val receivedTimestampMs: Long,
    val schemaVersion: Int = 1,
) {
    init {
        require(ingestionPath.isNotBlank()) {
            "Ingestion path must not be blank"
        }

        require(observationTimestampMs >= 0) {
            "Observation timestamp must be non-negative"
        }

        require(receivedTimestampMs >= 0) {
            "Received timestamp must be non-negative"
        }
    }
}

enum class EvidenceSourceType {
    ANDROID_WIFI_SCAN,
    ANDROID_BLE_SCAN,
    ANDROID_CELLULAR,
    DESKTOP_IMPORT,
    USB_TETHER,
    BLE_TETHER,
    MANUAL_IMPORT,
}
