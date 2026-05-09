package com.wscanplus.core.ingestion

import com.wscanplus.core.evidence.EvidenceSourceType

/**
 * Transport-agnostic observation ingestion source.
 *
 * Ingestion sources normalize how observations enter the pipeline while
 * remaining separate from:
 * - detector logic,
 * - confidence scoring,
 * - attribution,
 * - threat assessment.
 *
 * These abstractions intentionally avoid transport-specific detector behavior.
 */
interface ObservationIngestionSource {
    val sourceId: String
    val sourceType: EvidenceSourceType
    val transport: ObservationTransport
    val displayName: String
    val isUserInitiated: Boolean
}

enum class ObservationTransport {
    LOCAL_ANDROID,
    DESKTOP_IMPORT,
    USB_TETHER,
    BLE_TETHER,
    MANUAL_IMPORT,
}

/**
 * Local Android observation source.
 */
data class AndroidObservationSource(
    override val sourceId: String,
    override val sourceType: EvidenceSourceType,
    override val displayName: String,
    override val isUserInitiated: Boolean = false,
) : ObservationIngestionSource {
    override val transport: ObservationTransport = ObservationTransport.LOCAL_ANDROID
}

/**
 * Desktop-imported observation source.
 */
data class DesktopImportSource(
    override val sourceId: String,
    override val displayName: String,
    override val isUserInitiated: Boolean = true,
) : ObservationIngestionSource {
    override val sourceType: EvidenceSourceType = EvidenceSourceType.DESKTOP_IMPORT
    override val transport: ObservationTransport = ObservationTransport.DESKTOP_IMPORT
}

/**
 * USB-tethered companion ingestion source.
 */
data class UsbTetherSource(
    override val sourceId: String,
    override val displayName: String,
    override val isUserInitiated: Boolean = true,
) : ObservationIngestionSource {
    override val sourceType: EvidenceSourceType = EvidenceSourceType.USB_TETHER
    override val transport: ObservationTransport = ObservationTransport.USB_TETHER
}

/**
 * BLE-tethered companion ingestion source.
 */
data class BleTetherSource(
    override val sourceId: String,
    override val displayName: String,
    override val isUserInitiated: Boolean = true,
) : ObservationIngestionSource {
    override val sourceType: EvidenceSourceType = EvidenceSourceType.BLE_TETHER
    override val transport: ObservationTransport = ObservationTransport.BLE_TETHER
}
