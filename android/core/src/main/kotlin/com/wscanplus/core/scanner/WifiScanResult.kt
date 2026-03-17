package com.wscanplus.core.scanner

/**
 * Phase 1 Wi-Fi scan result model.
 *
 * All fields are available on minSdk 24:
 *   ssid/bssid/signalLevel/frequencyMhz/capabilities — API 1+
 *   timestamp — API 17 (microseconds since boot when result was last seen)
 *   channelWidth / centerFreq0 / centerFreq1 — API 23 (channel bandwidth + center frequencies)
 *
 * channelWidth values: ScanResult.CHANNEL_WIDTH_20MHZ=0, 40=1, 80=2, 160=3, 80PLUS80=4, 320=5
 * centerFreq1 is only meaningful for 80+80 MHz (CHANNEL_WIDTH_80MHZ_PLUS_MHZ); 0 otherwise.
 *
 * Phase 2 additions: wifiStandard (API 30), securityTypes (API 33), informationElements (API 30).
 */
data class WifiScanResult(
    val ssid: String,
    val bssid: String,
    val signalLevel: Int,
    val frequencyMhz: Int,
    val capabilities: String,
    val timestamp: Long,
    val channelWidth: Int,
    val centerFreq0: Int,
    val centerFreq1: Int,
)

fun interface ScanResultsListener {
    fun onResults(results: List<WifiScanResult>)
}
