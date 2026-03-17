package com.wscanplus.core.scanner

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * StandardScanner is the guaranteed baseline scanner — works on all devices, all API levels.
 *
 * Pattern:
 *   API 30+: registerScanResultsCallback() (non-deprecated, preferred)
 *   API 24–29: BroadcastReceiver for SCAN_RESULTS_AVAILABLE_ACTION + getScanResults()
 *
 * Note: WifiManager.startScan() is deprecated API 28 — only used on API < 28.
 *
 * Runtime permissions required (declared in Phase 1 permissions PR):
 *   ACCESS_FINE_LOCATION (all API levels), NEARBY_WIFI_DEVICES with neverForLocation (API 33+)
 *
 * Threading rule: start() and stop() MUST be called from a background thread.
 * start() is idempotent — safe to call multiple times.
 *
 * Callback threading: onResults() is delivered on the dedicated executor thread on both paths.
 * API 30+: executor passed to registerScanResultsCallback(). API 24–29: BroadcastReceiver
 * dispatches onto the same executor via executor.execute(). Both paths are off the main thread.
 *
 * TODO (Phase 1): handle permission-not-granted case gracefully.
 */
class StandardScanner(
    context: Context,
    private val resultsListener: ScanResultsListener = ScanResultsListener { },
) {
    private val appContext = context.applicationContext
    private val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private var receiver: BroadcastReceiver? = null
    private var scanResultsCallback: WifiManager.ScanResultsCallback? = null
    private var scanCallbackExecutor: ExecutorService? = null

    @RequiresPermission(
        allOf = [
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
        ],
    )
    fun start() {
        // idempotent — already started
        if (receiver != null || scanResultsCallback != null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startWithScanResultsCallback()
        } else {
            startWithBroadcastReceiver()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("MissingPermission")
    private fun startWithScanResultsCallback() {
        val executor =
            Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "wscanplus-standard-scanner")
            }
        val callback =
            object : WifiManager.ScanResultsCallback() {
            override fun onScanResultsAvailable() {
                val results = wifiManager.scanResults
                resultsListener.onResults(results.map { it.toWifiScanResult() })
            }
        }
        // Assign fields only after successful registration to prevent partial state on throw.
        try {
            wifiManager.registerScanResultsCallback(executor, callback)
            scanCallbackExecutor = executor
            scanResultsCallback = callback
        } catch (e: RuntimeException) {
            // Ensure we do not leak the executor thread if registration fails.
            executor.shutdownNow()
            throw e
        }
    }

    private fun startWithBroadcastReceiver() {
        if (scanCallbackExecutor == null) {
            scanCallbackExecutor =
                Executors.newSingleThreadExecutor { runnable ->
                    Thread(runnable, "wscanplus-standard-scanner")
                }
        }
        val executor = scanCallbackExecutor!!

        receiver =
            object : BroadcastReceiver() {
            // Permission verified by start() caller via @RequiresPermission contract.
            // Lint cannot trace through BroadcastReceiver.onReceive() system callbacks.
            @SuppressLint("MissingPermission")
            override fun onReceive(
                ctx: Context,
                intent: Intent,
            ) {
                if (intent.action != WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) return
                executor.execute {
                    val results = wifiManager.scanResults
                    resultsListener.onResults(results.map { it.toWifiScanResult() })
                }
            }
        }
        appContext.registerReceiver(
            receiver,
            IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION),
        )
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            @Suppress("DEPRECATION")
            wifiManager.startScan()
        }
    }

    fun stop() {
        // Balanced: receiver is only set by startWithBroadcastReceiver(), cleared here.
        // IllegalArgumentException should not occur given idempotent start(), but caught
        // defensively in case of unexpected lifecycle edge cases.
        receiver?.let {
            try {
                appContext.unregisterReceiver(it)
            } catch (e: IllegalArgumentException) {
                // Receiver was not registered — log in future when logging is available
            }
            receiver = null
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            scanResultsCallback?.let { callback ->
                try {
                    // Permission was held at start() time — same WifiManager session.
                    @SuppressLint("MissingPermission")
                    fun unregister() = wifiManager.unregisterScanResultsCallback(callback)
                    unregister()
                } catch (e: Exception) {
                    // Callback not registered or already unregistered — log in future when logging is available
                }
            }
        }
        scanResultsCallback = null
        scanCallbackExecutor?.shutdown()
        scanCallbackExecutor = null
    }

    @Suppress("DEPRECATION")
    private fun android.net.wifi.ScanResult.toWifiScanResult(): WifiScanResult {
        val ssid =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                wifiSsid?.toString()
            } else {
                SSID
            }
        return WifiScanResult(
            ssid = ssid ?: "",
            bssid = BSSID ?: "",
            signalLevel = level,
            frequencyMhz = frequency,
            capabilities = capabilities ?: "",
            timestamp = timestamp,
            channelWidth = channelWidth,
            centerFreq0 = centerFreq0,
            centerFreq1 = centerFreq1,
        )
    }
}
