package com.wscanplus.app

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.wscanplus.core.scanner.ScannerChain

/**
 * WatchdogService manages the scanner chain and the ADB communication socket.
 *
 * Foreground service — foregroundServiceType="dataSync" (declared in AndroidManifest).
 * Listens on localhost:9000 via java.net.ServerSocket for desktop ADB communication.
 * Serial number is the primary device key in all data structures.
 *
 * Android 15 constraint: dataSync foreground services have a 6-hour max runtime.
 * This service must handle a clean restart when that limit is reached (START_STICKY).
 *
 * Threading rule: scanner chain and ServerSocket operations MUST run on background
 * threads. Never call scanners or socket operations on the main thread.
 *
 * TODO (Phase 1 — permissions PR): call startForeground() with a notification once
 * FOREGROUND_SERVICE and FOREGROUND_SERVICE_DATA_SYNC permissions are declared.
 * TODO (Phase 1): launch scanner chain on a background thread (HandlerThread or coroutine).
 * TODO (Phase 3): open ServerSocket(9000) on a background thread for ADB comms.
 */
class WatchdogService : Service() {

    private var scannerChain: ScannerChain? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // TODO: startForeground(NOTIFICATION_ID, buildNotification()) — requires permissions PR
        scannerChain = ScannerChain(applicationContext)
        // TODO: scannerChain!!.start() on a background thread
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // TODO: scannerChain?.stop()
        // TODO: close ServerSocket
        scannerChain = null
    }
}
