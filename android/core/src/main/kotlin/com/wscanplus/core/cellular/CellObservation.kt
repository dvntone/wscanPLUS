package com.wscanplus.core.cellular

import com.wscanplus.core.evidence.EvidenceProvenance

/**
 * Passive normalized cellular observation.
 *
 * This model represents locally observed radio metadata only.
 *
 * It does not:
 * - identify devices,
 * - attribute actors,
 * - imply IMSI catcher detection,
 * - imply Stingray detection,
 * - imply lawful/intercept capability.
 *
 * The model is intentionally transport-agnostic so future desktop,
 * tethered, and imported observations can normalize into the same
 * deterministic aggregation pipeline.
 */
data class CellObservation(
    val observationId: String,
    val radioType: CellularRadioType,
    val mcc: String?,
    val mnc: String?,
    val trackingAreaCode: Int?,
    val cellId: Long?,
    val physicalCellId: Int?,
    val arfcn: Int?,
    val bandwidthKhz: Int?,
    val signalDbm: Int?,
    val timestampMs: Long,
    val provenance: EvidenceProvenance?,
    val schemaVersion: Int = 1,
) {
    init {
        require(observationId.isNotBlank()) {
            "Observation id must not be blank"
        }

        require(timestampMs >= 0) {
            "Timestamp must be non-negative"
        }

        require(mcc == null || MCC_REGEX.matches(mcc)) {
            "MCC must contain exactly 3 digits"
        }

        require(mnc == null || MNC_REGEX.matches(mnc)) {
            "MNC must contain 2 or 3 digits"
        }
    }

    companion object {
        private val MCC_REGEX = Regex("^[0-9]{3}$")
        private val MNC_REGEX = Regex("^[0-9]{2,3}$")
    }
}

enum class CellularRadioType {
    GSM,
    WCDMA,
    LTE,
    NR,
    CDMA,
    UNKNOWN,
}
