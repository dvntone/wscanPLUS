package com.wscanplus.app

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.wscanplus.core.scanner.ScannerChain
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
 * This service must handle a clean restart when that limit is reached (START_STICKY).
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

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
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
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
        if (scannerChain == null) {
            val executor =
                Executors.newSingleThreadExecutor { runnable ->
                    Thread(runnable, "wscanplus-watchdog")
                }
            scannerExecutor = executor
            scannerChain =
                ScannerChain(applicationContext) { _ ->
                    // Phase 1 stub — TODO (Phase 2): forward results to data layer / ADB channel.
                }
            startFuture = executor.submit { startChain(scannerChain!!) }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
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

    // MainActivity verifies permissions before calling startForegroundService(). However,
    // this service can also be restarted via START_STICKY after permission revocation, so
    // this assumption is not guaranteed at all entry points. A SecurityException from
    // chain.start() will propagate to the executor thread.
    // TODO (Phase 2): check permissions inside the service and stop gracefully if revoked.
    @SuppressLint("MissingPermission")
    private fun startChain(chain: ScannerChain) = chain.start()

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
            // Placeholder system icon — replace with app icon in Phase 2
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setOngoing(true)
            .build()

    companion object {
        private const val CHANNEL_ID = "wscanplus_watchdog"
        private const val NOTIFICATION_ID = 1
    }
}
