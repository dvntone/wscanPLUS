package com.wscanplus.core.scanner

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission

/**
 * ScannerChain orchestrates the scanner priority chain: USB > Standard.
 * Root scanner is a developer opt-in stub only — never a silent fallback.
 *
 * Chain logic:
 *   1. USB scanner — external OTG WiFi adapter (enhanced scanning, advanced users)
 *   2. Standard scanner — WifiManager BroadcastReceiver (guaranteed baseline, all devices)
 *   Root — dev opt-in only, never in the chain without an explicit flag
 *
 * Threading rule: start() and stop() MUST be called from a background thread.
 * This class does not manage threading itself — callers (WatchdogService) are responsible.
 *
 * TODO (Phase 1): implement USB adapter detection and chain switching.
 * TODO (Phase 1): wire scan results to WatchdogService via callback or channel.
 */
class ScannerChain(
    private val context: Context,
    private val resultsListener: ScanResultsListener = ScanResultsListener { },
) {
    private val usbScanner = UsbScanner(context)
    private val standardScanner = StandardScanner(context, resultsListener)
    // RootScanner is never instantiated in the chain — dev opt-in only.

    @RequiresPermission(
        allOf = [
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
        ],
    )
    fun start() {
        if (usbScanner.isAvailable()) {
            try {
                usbScanner.start()
                return
            } catch (e: Exception) {
                Log.e(TAG, "USB scanner start failed, falling back to standard scanner", e)
            }
        }
        standardScanner.start()
    }

    fun stop() {
        usbScanner.stop()
        standardScanner.stop()
    }

    companion object {
        private const val TAG = "ScannerChain"
    }
}
