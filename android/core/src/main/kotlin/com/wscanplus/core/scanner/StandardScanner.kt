package com.wscanplus.core.scanner

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * StandardScanner is the guaranteed baseline scanner — works on all devices, all API levels.
 *
 * Pattern:
 *   API 30+: registerScanResultsCallback() for result delivery + periodic startScan() requests
 *   API 24–29: BroadcastReceiver for SCAN_RESULTS_AVAILABLE_ACTION + periodic startScan() requests
 *
 * Note: WifiManager.startScan() is deprecated API 28 and throttled on newer Android versions.
 * Both paths still require explicit startScan() requests — registerScanResultsCallback() delivers
 * results but does not itself trigger scans. Without active requests the scanner is passive.
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
    private var scanScheduler: ScheduledExecutorService? = null

    @RequiresPermission(
        allOf = [
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
        ],
    )
    fun start() {
        // idempotent — already started
        if (receiver != null || scanResultsCallback != null || scanScheduler != null) return
        Log.i(TAG, "Scanner started (API ${Build.VERSION.SDK_INT})")
        if (usesLegacyBroadcastPath(Build.VERSION.SDK_INT)) {
            startWithBroadcastReceiver()
        } else {
            startWithScanResultsCallback()
        }
        scheduleActiveScans()
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
                    val freshResults = freshScanResults(results)
                    Log.i(TAG, "Received ${results.size} scan results (${freshResults.size} after stale filter)")
                    resultsListener.onResults(freshResults.map { it.toWifiScanResult() })
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
                        val freshResults = freshScanResults(results)
                        Log.i(TAG, "Received ${results.size} scan results (${freshResults.size} after stale filter)")
                        resultsListener.onResults(freshResults.map { it.toWifiScanResult() })
                    }
                }
            }
        appContext.registerReceiver(
            receiver,
            IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION),
        )
    }

    @SuppressLint("MissingPermission")
    private fun scheduleActiveScans() {
        val scheduler =
            Executors.newSingleThreadScheduledExecutor { runnable ->
                Thread(runnable, "wscanplus-standard-scanner-scan")
            }
        scanScheduler = scheduler
        scheduler.scheduleWithFixedDelay(
            {
                try {
                    @Suppress("DEPRECATION")
                    val accepted = wifiManager.startScan()
                    if (!accepted) {
                        Log.w(
                            TAG,
                            "WifiManager.startScan() request was not accepted " +
                                "(API ${Build.VERSION.SDK_INT}); scan may be throttled or rejected",
                        )
                    }
                } catch (e: RuntimeException) {
                    Log.w(TAG, "WifiManager.startScan() failed", e)
                }
            },
            0L,
            SCAN_REQUEST_INTERVAL_SECONDS,
            TimeUnit.SECONDS,
        )
    }

    fun stop() {
        Log.i(TAG, "Scanner stopped")
        // Balanced: receiver is only set by startWithBroadcastReceiver(), cleared here.
        // IllegalArgumentException should not occur given idempotent start(), but caught
        // defensively in case of unexpected lifecycle edge cases.
        receiver?.let {
            try {
                appContext.unregisterReceiver(it)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Receiver teardown skipped because it was not registered", e)
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
                    Log.w(TAG, "Scan callback teardown did not complete cleanly", e)
                }
            }
        }
        scanResultsCallback = null
        scanScheduler?.shutdownNow()
        scanScheduler = null
        scanCallbackExecutor?.shutdownNow()
        scanCallbackExecutor = null
    }

    private fun freshScanResults(results: List<android.net.wifi.ScanResult>): List<android.net.wifi.ScanResult> {
        val nowUs = SystemClock.elapsedRealtime() * 1000
        return results.filter { result: android.net.wifi.ScanResult ->
            nowUs - result.timestamp <= STALE_THRESHOLD_US
        }
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

    companion object {
        private const val TAG = "StandardScanner"
        private const val STALE_THRESHOLD_US = 120_000_000L
        private const val SCAN_REQUEST_INTERVAL_SECONDS = 30L

        internal fun usesLegacyBroadcastPath(apiLevel: Int): Boolean = apiLevel < Build.VERSION_CODES.R
    }
}
