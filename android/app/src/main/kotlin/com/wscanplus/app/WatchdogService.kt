package com.wscanplus.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.wscanplus.core.scanner.ScannerChain
import com.wscanplus.core.scanner.WifiScanResult
import com.wscanplus.core.scanner.toScanInput
import com.wscanplus.core.threat.BssidFingerprintHeuristic
import com.wscanplus.core.threat.EncryptionDowngradeHeuristic
import com.wscanplus.core.threat.EnvironmentType
import com.wscanplus.core.threat.EvilTwinHeuristic
import com.wscanplus.core.threat.HeuristicEngine
import com.wscanplus.core.threat.KarmaHeuristic
import com.wscanplus.core.threat.PolicyGate
import com.wscanplus.core.threat.RssiAnomalyHeuristic
import com.wscanplus.core.threat.ScanContext
import com.wscanplus.core.threat.ScanInput
import com.wscanplus.core.threat.SsidFloodingHeuristic
import com.wscanplus.core.threat.WepOpenHeuristic
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * WatchdogService manages the scanner chain and the ADB communication socket.
 *
 * Foreground service — foregroundServiceType="dataSync" (declared in AndroidManifest).
 * Must call startForeground() within a few seconds of startForegroundService() or the
 * system throws ForegroundServiceDidNotStartInTimeException (API 31+).
 *
 * Serial number is the primary device key in all data structures.
 *
 * Android 15 constraint: dataSync foreground services have a 6-hour max runtime.
 * On API 35+, this service schedules a clean restart at 5h 50min to reset the timer.
 * On older APIs, no restart is needed (no runtime limit).
 *
 * Threading rule: scanner chain and ServerSocket operations MUST run on background
 * threads. Never call scanners or socket operations on the main thread.
 *
 * TODO (Phase 3): open ServerSocket(9000) on a background thread for ADB comms.
 */
class WatchdogService : Service() {
    private var scannerChain: ScannerChain? = null
    private var scannerExecutor: ExecutorService? = null
    private var startFuture: Future<*>? = null
    private var startTimeMillis: Long = 0L
    private val handler = Handler(Looper.getMainLooper())
    private val restartRunnable = Runnable { performScheduledRestart() }
    private val engine =
        HeuristicEngine(
            listOf(
                WepOpenHeuristic(),
                EvilTwinHeuristic(),
                EncryptionDowngradeHeuristic(),
                KarmaHeuristic(),
                SsidFloodingHeuristic(),
                RssiAnomalyHeuristic(),
                BssidFingerprintHeuristic(ouiLookup = null),
            ),
        )
    private val policyGate = PolicyGate()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "Service created")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // FOREGROUND_SERVICE_TYPE_DATA_SYNC is API 29 — safe to inline: ServiceCompat.startForeground()
    // guards the type parameter internally and falls back to startForeground(id, notification)
    // on API < 29. The constant value is copied at compile time and causes no runtime issue.
    @SuppressLint("InlinedApi")
    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (hasLocationPermission().not()) {
            Log.w(TAG, "Permission denied, stopping")
            stopSelfResult(startId)
            return START_NOT_STICKY
        }
        Log.d(TAG, "Service started")
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
        if (scannerChain == null) {
            startTimeMillis = SystemClock.elapsedRealtime()
            val executor =
                Executors.newSingleThreadExecutor { runnable ->
                    Thread(runnable, "wscanplus-watchdog")
                }
            scannerExecutor = executor
            scannerChain =
                ScannerChain(applicationContext) { results: List<WifiScanResult> ->
                    val scanInputs: List<ScanInput> =
                        results.map { result: WifiScanResult ->
                            result.toScanInput()
                        }
                    val context =
                        ScanContext(
                            currentResults = scanInputs,
                            knownProfiles = emptyMap(),
                            baselineNetworkCount = null,
                            baselineStdDev = null,
                            environmentType = EnvironmentType.RESIDENTIAL,
                        )
                    val rawSignals = engine.analyze(context)
                    val filtered = policyGate.filter(rawSignals)
                    Log.d(
                        "WatchdogService",
                        "Threats: ${filtered.size} of ${rawSignals.size} signals passed policy gate",
                    )
                }
            startFuture = executor.submit { startChain(scannerChain!!) }
            scheduleDataSyncRestart()
        }
        return START_STICKY
    }

    private fun hasLocationPermission(): Boolean =
        checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        handler.removeCallbacks(restartRunnable)
        // Cancel any queued start task before submitting stop, so a pending start cannot
        // race with or follow the stop on the executor queue.
        startFuture?.cancel(true)
        startFuture = null
        val executor = scannerExecutor
        val chain = scannerChain
        scannerChain = null
        scannerExecutor = null
        // stop() must run on a background thread per ScannerChain/StandardScanner threading rule.
        executor?.execute { chain?.stop() }
        executor?.shutdown()
        // TODO (Phase 3): close ServerSocket
    }

    // onStartCommand() re-validates runtime location permission before invoking the chain.
    // This suppresses lint for the background-thread call path after that guard succeeds.
    @SuppressLint("MissingPermission")
    private fun startChain(chain: ScannerChain) = chain.start()

    /**
     * Android 15+ (API 35): dataSync foreground services have a 6-hour max runtime.
     * Schedule a clean restart 10 minutes before the limit to avoid being killed.
     * On older APIs, this is a no-op.
     */
    private fun scheduleDataSyncRestart() {
        if (Build.VERSION.SDK_INT < ANDROID_15_API) return
        handler.postDelayed(restartRunnable, RESTART_DELAY_MS)
        Log.d(TAG, "Scheduled dataSync restart in ${RESTART_DELAY_MS / 1000 / 60} minutes")
    }

    /**
     * Performs a clean restart: stops the scanner chain, stops self, then starts a new
     * service instance. The new instance gets a fresh 6-hour runtime window.
     */
    private fun performScheduledRestart() {
        Log.d(TAG, "Performing scheduled dataSync restart after ${uptimeMinutes()} minutes")
        val restartIntent = Intent(this, WatchdogService::class.java)
        stopSelf()
        ContextCompat.startForegroundService(this, restartIntent)
    }

    private fun uptimeMinutes(): Long {
        val elapsed = SystemClock.elapsedRealtime() - startTimeMillis
        return elapsed / 1000 / 60
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "wscan+ Scanner",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Active while the WiFi scanner chain is running"
                }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification() =
        NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setContentTitle("wscan+ scanning")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()

    companion object {
        private const val TAG = "WatchdogService"
        private const val CHANNEL_ID = "wscanplus_watchdog"
        private const val NOTIFICATION_ID = 1
        private const val ANDROID_15_API = 35

        // 5 hours 50 minutes in milliseconds — restart 10 min before 6h limit
        private const val RESTART_DELAY_MS = 5L * 60 * 60 * 1000 + 50 * 60 * 1000
    }
}
