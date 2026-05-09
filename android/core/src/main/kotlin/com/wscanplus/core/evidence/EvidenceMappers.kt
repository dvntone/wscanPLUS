package com.wscanplus.core.evidence

import com.wscanplus.core.scanner.WifiScanResult
import com.wscanplus.core.scanner.toScanInput
import com.wscanplus.core.threat.ScanInput
import com.wscanplus.core.threat.ThreatSignal

fun WifiScanResult.toObservationEvent(id: String? = null): ObservationEvent {
    val scanInput = toScanInput()
    val observationId = id ?: wifiObservationId(scanInput.bssid, scanInput.timestamp)
    return scanInput.toObservationEvent(
        id = observationId,
        observedAtBasis = TimeBasis.ELAPSED_REALTIME,
    )
}

fun ScanInput.toObservationEvent(
    observedAtBasis: TimeBasis,
    id: String = wifiObservationId(bssid, timestamp),
): ObservationEvent =
    ObservationEvent(
        id = id,
        source = ObservationSource.ANDROID_WIFI,
        observedAtMs = timestamp,
        observedAtBasis = observedAtBasis,
        wifi =
            WifiObservationEvidence(
                ssid = ssid,
                bssid = bssid,
                hiddenSsid = isHidden,
                rssiDbm = rssiDbm,
                frequencyMhz = frequencyMhz,
                channelWidth = channelWidth,
                capabilities = capabilities,
            ),
    )

fun ThreatSignal.toDetectionEvidenceEvent(
    observation: ObservationEvent,
    id: String = detectionEvidenceId(bssid, detectedAt, heuristicType?.name),
): DetectionEvidenceEvent {
    require(observation.wifi.bssid.equals(bssid, ignoreCase = true)) {
        "Observation BSSID must match threat signal BSSID"
    }

    return DetectionEvidenceEvent(
        id = id,
        observationId = observation.id,
        source = source,
        heuristicType = heuristicType,
        confidence = confidence,
        reasons = reasons,
        bssid = bssid,
        detectedAtMs = detectedAt,
        provenance = null,
    )
}

private fun wifiObservationId(
    bssid: String,
    observedAtMs: Long,
): String = "${ObservationSource.ANDROID_WIFI.contractValue}:${bssid.lowercase()}:$observedAtMs"

private fun detectionEvidenceId(
    bssid: String,
    detectedAtMs: Long,
    heuristicType: String?,
): String = "detection:${bssid.lowercase()}:${heuristicType ?: "unknown"}:$detectedAtMs"
