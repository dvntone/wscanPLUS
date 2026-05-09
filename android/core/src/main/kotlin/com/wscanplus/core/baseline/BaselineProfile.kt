package com.wscanplus.core.baseline

/**
 * Trusted wireless baseline for a labeled zone.
 *
 * This model is intentionally small for the first detector slice. Persistence,
 * enrollment UX, and multi-source baselines can build on top of it later.
 */
data class BaselineProfile(
    val id: String,
    val zoneLabel: String,
    val trustedAccessPoints: List<TrustedApProfile>,
    val schemaVersion: Int = 1,
)

data class TrustedApProfile(
    val ssid: String,
    val bssid: String,
    val expectedFrequenciesMhz: Set<Int>,
    val expectedCapabilities: Set<String>,
    val rssiMedianDbm: Int? = null,
    val rssiToleranceDb: Int? = null,
) {
    init {
        require(rssiToleranceDb == null || rssiToleranceDb >= 0) {
            "RSSI tolerance must be non-negative"
        }
    }
}
