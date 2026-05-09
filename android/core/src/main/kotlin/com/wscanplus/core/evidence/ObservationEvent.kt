package com.wscanplus.core.evidence

/**
 * Canonical factual observation captured by wscan+.
 *
 * Observation events are facts only. They do not imply malicious activity,
 * attribution, or detector confidence by themselves.
 *
 * `observedAtMs` preserves the timestamp supplied by the source adapter. Use
 * `observedAtBasis` to determine whether that value is elapsed realtime or
 * wall-clock epoch milliseconds.
 */
data class ObservationEvent(
    val id: String,
    val source: ObservationSource,
    val observedAtMs: Long,
    val observedAtBasis: TimeBasis,
    val wifi: WifiObservationEvidence,
    val schemaVersion: Int = 1,
)

enum class ObservationSource(
    val contractValue: String,
) {
    ANDROID_WIFI("android_wifi"),
}

enum class TimeBasis {
    ELAPSED_REALTIME,
    EPOCH,
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
