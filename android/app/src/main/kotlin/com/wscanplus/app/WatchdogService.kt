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
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
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
 * Foreground service — foregroundServiceType includes location|dataSync.
 * Must call startForeground() within a few seconds of startForegroundService() or the
 * system throws ForegroundServiceDidNotStartInTimeException (API 31+).
 *
 * Serial number is the primary device key in all data structures.
 *
 * Android 15 constraint: the retained dataSync role still has a 6-hour runtime budget in a
 * rolling 24-hour window. On API 35+, the service must handle onTimeout() and stop cleanly.
 * A pre-timeout restart does not reset that budget unless the user brings the app foreground,
 * so the correct hardening behavior is graceful shutdown rather than a restart loop.
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

    // FOREGROUND_SERVICE_TYPE_* constants are API 29 — safe to inline: ServiceCompat.startForeground()
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
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
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
        }
        return START_STICKY
    }

    private fun hasLocationPermission(): Boolean =
        checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
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
     * Android 15+ (API 35): dataSync foreground services receive onTimeout() once their
     * rolling 24-hour budget is exhausted. Stop within the grace window so the system
     * does not terminate the process with a timeout failure.
     */
    override fun onTimeout(
        startId: Int,
        fgsType: Int,
    ) {
        Log.w(
            TAG,
            "Foreground service timeout after ${uptimeMinutes()} minutes " +
                "(startId=$startId, fgsType=0x${fgsType.toString(16)}), stopping service",
        )
        stopSelf()
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
    }
}
