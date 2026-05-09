package com.wscanplus.core.evidence

import com.wscanplus.core.threat.HeuristicType
import com.wscanplus.core.threat.ThreatSource

/**
 * Canonical detector evidence derived from existing threat signals.
 *
 * This model preserves existing detector confidence and reasons. It does not
 * upgrade confidence or imply attribution beyond the source signal.
 *
 * `detectedAtMs` is wall-clock epoch milliseconds from `ThreatSignal.detectedAt`.
 * It does not necessarily share the same time basis as the linked observation.
 */
data class DetectionEvidenceEvent(
    val id: String,
    val observationId: String,
    val source: ThreatSource,
    val heuristicType: HeuristicType?,
    val confidence: Float,
    val reasons: List<String>,
    val bssid: String,
    val detectedAtMs: Long,
    val schemaVersion: Int = 1,
) {
    init {
        require(confidence in 0.0f..0.95f) {
            "Confidence must be 0.0–0.95, was $confidence"
        }
        require(reasons.size <= 3) {
            "Maximum 3 reasons, was ${reasons.size}"
        }
    }
}
