package com.wscanplus.core.scanner

/**
 * Phase 1 minimal Wi-Fi scan result model.
 * Additional fields can be added as needed in later phases.
 */
data class WifiScanResult(
    val ssid: String,
    val bssid: String,
    val signalLevel: Int,
    val frequencyMhz: Int,
    val capabilities: String
)

fun interface ScanResultsListener {
    fun onResults(results: List<WifiScanResult>)
}
