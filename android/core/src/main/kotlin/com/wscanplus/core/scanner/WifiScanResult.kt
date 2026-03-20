package com.wscanplus.core.scanner

import com.wscanplus.core.threat.ScanInput

/**
 * Phase 1 Wi-Fi scan result model.
 *
 * All fields are available on minSdk 24:
 *   ssid/bssid/signalLevel/frequencyMhz/capabilities — API 1+
 *   timestamp — API 17 (microseconds since boot when result was last seen)
 *   channelWidth / centerFreq0 / centerFreq1 — API 23 (channel bandwidth + center frequencies)
 *
 * channelWidth values: ScanResult.CHANNEL_WIDTH_20MHZ, CHANNEL_WIDTH_40MHZ, CHANNEL_WIDTH_80MHZ,
 *   CHANNEL_WIDTH_160MHZ, CHANNEL_WIDTH_80MHZ_PLUS_MHZ, CHANNEL_WIDTH_320MHZ.
 * centerFreq1 is only meaningful for ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ; 0 otherwise.
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

fun WifiScanResult.toScanInput(): ScanInput =
    ScanInput(
        bssid = bssid,
        ssid = ssid,
        isHidden = ssid.isBlank(),
        capabilities = capabilities,
        rssiDbm = signalLevel,
        frequencyMhz = frequencyMhz,
        channelWidth = channelWidth,
        timestamp = timestamp,
    )

fun interface ScanResultsListener {
    fun onResults(results: List<WifiScanResult>)
}
