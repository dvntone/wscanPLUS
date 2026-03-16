package com.wscanplus.core.scanner

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Build
import androidx.annotation.RequiresPermission

/**
 * StandardScanner is the guaranteed baseline scanner — works on all devices, all API levels.
 *
 * Pattern:
 *   API 30+: registerScanResultsCallback() (non-deprecated, preferred)
 *   API 24–29: BroadcastReceiver for SCAN_RESULTS_AVAILABLE_ACTION + getScanResults()
 *
 * Note: WifiManager.startScan() is deprecated API 28 — NOT used here.
 *
 * Runtime permissions required (declared in Phase 1 permissions PR):
 *   ACCESS_FINE_LOCATION (all API levels), NEARBY_WIFI_DEVICES with neverForLocation (API 33+)
 *
 * Threading rule: start() and stop() MUST be called from a background thread.
 * start() is idempotent — safe to call multiple times.
 *
 * TODO (Phase 1): implement registerScanResultsCallback() path for API 30+.
 * TODO (Phase 1): forward scan results via callback or channel to ScannerChain.
 * TODO (Phase 1): handle permission-not-granted case gracefully.
 */
class StandardScanner(private val context: Context) {

    private var receiver: BroadcastReceiver? = null

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.ACCESS_FINE_LOCATION])
    fun start() {
        if (receiver != null) return  // idempotent — already started
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+: use registerScanResultsCallback — TODO implement
        } else {
            startWithBroadcastReceiver()
        }
    }

    private fun startWithBroadcastReceiver() {
        receiver = object : BroadcastReceiver() {
            // Permission verified by start() caller via @RequiresPermission contract.
            // Lint cannot trace through BroadcastReceiver.onReceive() system callbacks.
            @SuppressLint("MissingPermission")
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action != WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) return
                val wifiManager = ctx.applicationContext
                    .getSystemService(Context.WIFI_SERVICE) as WifiManager
                @Suppress("DEPRECATION", "UNUSED_VARIABLE")
                val results = wifiManager.scanResults
                // TODO: forward results to ScannerChain callback
            }
        }
        context.registerReceiver(
            receiver,
            IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        )
    }

    fun stop() {
        // Balanced: receiver is only set by startWithBroadcastReceiver(), cleared here.
        // IllegalArgumentException should not occur given idempotent start(), but caught
        // defensively in case of unexpected lifecycle edge cases.
        receiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: IllegalArgumentException) {
                // Receiver was not registered — log in future when logging is available
            }
            receiver = null
        }
        // TODO: unregister registerScanResultsCallback when API 30+ path is implemented
    }
}
