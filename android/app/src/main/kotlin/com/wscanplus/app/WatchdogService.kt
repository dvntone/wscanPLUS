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
 * TODO (Phase 1): launch scanner chain on a background thread (HandlerThread or coroutine).
 * TODO (Phase 3): open ServerSocket(9000) on a background thread for ADB comms.
 */
class WatchdogService : Service() {

    private var scannerChain: ScannerChain? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // FOREGROUND_SERVICE_TYPE_DATA_SYNC is API 29 — safe to inline: ServiceCompat.startForeground()
    // guards the type parameter internally and falls back to startForeground(id, notification)
    // on API < 29. The constant value is copied at compile time and causes no runtime issue.
    @SuppressLint("InlinedApi")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
        if (scannerChain == null) {
            scannerChain = ScannerChain(applicationContext)
            // TODO (Phase 1): scannerChain!!.start() on a background thread
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        scannerChain?.stop()
        // TODO (Phase 3): close ServerSocket
        scannerChain = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "wscan+ Scanner",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active while the WiFi scanner chain is running"
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
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
