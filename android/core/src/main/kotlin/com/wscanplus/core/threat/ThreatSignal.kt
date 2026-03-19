package com.wscanplus.core.threat

enum class ThreatSource { LOCAL_HEURISTIC, CROWDSEC_CTI, GEMINI }

enum class HeuristicType {
    WEP_OPEN,
    EVIL_TWIN,
    ENCRYPTION_DOWNGRADE,
    KARMA_ATTACK,
    SSID_FLOODING,
    RSSI_ANOMALY,
    BSSID_FINGERPRINT,
}

data class ThreatSignal(
    val confidence: Float,
    val source: ThreatSource,
    val reasons: List<String>,
    val heuristicType: HeuristicType?,
    val bssid: String,
    val detectedAt: Long = System.currentTimeMillis(),
    val schemaVersion: Int = 1,
) {
    init {
        require(confidence in 0.0f..0.95f) {
            "Confidence must be 0.0\u20130.95, was $confidence"
        }
        require(reasons.size <= 3) {
            "Maximum 3 reasons, was ${reasons.size}"
        }
    }
}
