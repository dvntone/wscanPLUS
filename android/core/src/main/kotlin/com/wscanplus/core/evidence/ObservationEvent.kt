package com.wscanplus.core.evidence

/**
 * Canonical factual observation captured by wscan+.
 *
 * Observation events are facts only. They do not imply malicious activity,
 * attribution, or detector confidence by themselves.
 */
data class ObservationEvent(
    val id: String,
    val source: ObservationSource,
    val observedAtMs: Long,
    val wifi: WifiObservationEvidence,
    val schemaVersion: Int = 1,
)

enum class ObservationSource {
    ANDROID_WIFI_SCAN,
}

data class WifiObservationEvidence(
    val ssid: String,
    val bssid: String,
    val hiddenSsid: Boolean,
    val rssiDbm: Int,
    val frequencyMhz: Int,
    val channelWidth: Int,
    val capabilities: String,
)
