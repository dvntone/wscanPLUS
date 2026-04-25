package com.wscanplus.app

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.TextView
import com.wscanplus.app.db.DbPassphraseProvider
import com.wscanplus.core.db.WscanDatabase
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.util.concurrent.Executors

class ScanMapActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    private var watchdogService: WatchdogService? = null
    private var floorBadge: TextView? = null
    private var statusPanel: View? = null
    private var statusTitle: TextView? = null
    private var statusBody: TextView? = null
    private var localHeatmapOverlay: LocalHeatmapView? = null
    private var isServiceBound = false

    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName,
                service: IBinder,
            ) {
                watchdogService = (service as WatchdogService.LocalBinder).getService()
                scheduleFloorUpdate()
            }

            override fun onServiceDisconnected(name: ComponentName) {
                watchdogService = null
                isServiceBound = false
                handler.removeCallbacks(floorUpdateRunnable)
                floorBadge?.visibility = View.GONE
            }
        }

    private val floorUpdateRunnable: Runnable =
        object : Runnable {
            override fun run() {
                updateFloorBadge()
                handler.postDelayed(this, FLOOR_UPDATE_INTERVAL_MS)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WscanUi.prepareWindow(this)
        setContentView(R.layout.activity_scan_map)

        WscanUi.applySystemInsets(
            findViewById(R.id.scan_map_root),
            horizontalPadding = 0,
            topPadding = 0,
            bottomPadding = 0,
        )
        floorBadge = findViewById(R.id.floor_badge)
        statusPanel = findViewById(R.id.map_status_panel)
        statusTitle = findViewById(R.id.map_status_title)
        statusBody = findViewById(R.id.map_status_body)
        localHeatmapOverlay = findViewById(R.id.local_heatmap_overlay)

        setMapStatus("Loading scan map", "Loading GPS-tagged scan records from encrypted database.")
        loadHeatmap()
    }

    override fun onStart() {
        super.onStart()
        val bindIntent = Intent(this, WatchdogService::class.java)
        isServiceBound = bindService(bindIntent, serviceConnection, 0)
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(floorUpdateRunnable)
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
        watchdogService = null
    }

    private fun scheduleFloorUpdate() {
        handler.removeCallbacks(floorUpdateRunnable)
        handler.post(floorUpdateRunnable)
    }

    private fun updateFloorBadge() {
        val badge = floorBadge ?: return
        val estimate = watchdogService?.currentFloorEstimate
        if (estimate == null) {
            badge.visibility = View.GONE
            return
        }
        val floorLabel =
            when {
                estimate.relativeFloor > 0 -> "Floor +${estimate.relativeFloor}"
                estimate.relativeFloor < 0 -> "Floor ${estimate.relativeFloor}"
                else -> "Floor 0"
            }
        badge.text = "$floorLabel (±${estimate.confidenceMeters.toInt()}m)"
        badge.visibility = View.VISIBLE
    }

    private fun loadHeatmap() {
        executor.execute {
            try {
                renderHeatmap()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load scan map data", e)
                if (!isDestroyed) {
                    runOnUiThread {
                        setMapStatus(
                            "Map data unavailable",
                            "Failed to load scan map data. Check database state.",
                        )
                    }
                }
            }
        }
    }

    private fun renderHeatmap() {
        val passphrase = DbPassphraseProvider(applicationContext).getOrCreate()
        if (passphrase == null) {
            Log.e(TAG, "Encrypted database unavailable — cannot load map data")
            if (!isDestroyed) {
                runOnUiThread {
                    localHeatmapOverlay?.clearPoints()
                    setMapStatus(
                        "Encrypted database unavailable",
                        "The scan database could not be opened.",
                    )
                }
            }
            return
        }

        SQLiteDatabase.loadLibs(applicationContext)
        val db = WscanDatabase.getInstance(applicationContext, SupportFactory(passphrase))
        val scanResults = db.scanResultDao().getGpsTagged(limit = 500)
        if (scanResults.isEmpty()) {
            if (!isDestroyed) {
                runOnUiThread {
                    localHeatmapOverlay?.clearPoints()
                    setMapStatus(
                        "No GPS-tagged scan data",
                        "Run a scan with location enabled to populate the heatmap.",
                    )
                }
            }
            return
        }

        val rawSignals = db.threatSignalDao().getHighConfidence(minConfidence = 0.3f, limit = 1000)
        val threatMap =
            rawSignals
                .groupBy { signal -> signal.bssid }
                .mapValues { (_, signalList) -> signalList.maxOf { signal -> signal.confidence } }

        val points =
            scanResults.mapNotNull { result ->
                val lat = result.latitude ?: return@mapNotNull null
                val lng = result.longitude ?: return@mapNotNull null
                LocalHeatmapView.Point(lat, lng, (threatMap[result.bssid] ?: 0.1f).toDouble())
            }

        if (points.isEmpty()) {
            if (!isDestroyed) {
                runOnUiThread {
                    localHeatmapOverlay?.clearPoints()
                    setMapStatus(
                        "Scan records lack coordinates",
                        "Stored scan rows exist but none include GPS coordinates yet.",
                    )
                }
            }
            return
        }

        if (!isDestroyed) {
            runOnUiThread {
                localHeatmapOverlay?.setPoints(points)
                setMapStatus(
                    "Heatmap ready",
                    "${points.size} GPS-tagged scan points loaded.",
                )
            }
        }
    }

    private fun setMapStatus(
        title: String,
        body: String,
    ) {
        statusTitle?.text = title
        statusBody?.text = body
        statusPanel?.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdownNow()
    }

    companion object {
        private const val TAG = "ScanMapActivity"
        private const val FLOOR_UPDATE_INTERVAL_MS = 1000L
    }
}
