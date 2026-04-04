package com.wscanplus.app

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
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.google.maps.android.heatmaps.WeightedLatLng
import com.wscanplus.app.db.DbPassphraseProvider
import com.wscanplus.core.db.WscanDatabase
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.util.concurrent.Executors

/**
 * Displays a GPS-tagged scan history heatmap using Google Maps.
 *
 * Each point corresponds to a scan result with a recorded GPS location.
 * Points are weighted by the maximum threat signal confidence for that BSSID — threat
 * detections appear as hot zones; clean BSSIDs contribute minimal baseline heat.
 *
 * When WatchdogService is running and the device has a barometer, a floor badge is
 * shown in the top-left corner reflecting the current relative floor estimate.
 *
 * Requires GOOGLE_MAPS_API_KEY in local.properties (injected via secrets-gradle-plugin).
 */
class ScanMapActivity :
    FragmentActivity(),
    OnMapReadyCallback {
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    private var watchdogService: WatchdogService? = null
    private var floorBadge: TextView? = null
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
        setContentView(R.layout.activity_scan_map)

        floorBadge = findViewById(R.id.floor_badge)

        val mapFragment = SupportMapFragment.newInstance()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.map_container, mapFragment)
            .commit()
        mapFragment.getMapAsync(this)
    }

    override fun onStart() {
        super.onStart()
        // Bind only if the service is already running — do not create it just for the badge.
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
        val confidenceLabel = "±${estimate.confidenceMeters.toInt()}m"
        badge.text = "$floorLabel ($confidenceLabel)"
        badge.visibility = View.VISIBLE
    }

    override fun onMapReady(map: GoogleMap) {
        executor.execute {
            try {
                loadAndRender(map)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load scan map data", e)
                if (!isDestroyed) {
                    runOnUiThread {
                        Toast.makeText(this, "Failed to load map data.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun loadAndRender(map: GoogleMap) {
        val passphrase = DbPassphraseProvider(applicationContext).getOrCreate()
        if (passphrase == null) {
            Log.e(TAG, "Encrypted database unavailable — cannot load map data")
            if (!isDestroyed) {
                runOnUiThread {
                    Toast.makeText(this, "Encrypted database unavailable.", Toast.LENGTH_LONG).show()
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
                    Toast.makeText(this, "No GPS-tagged scan data yet.", Toast.LENGTH_LONG).show()
                }
            }
            return
        }

        // BSSID → max threat confidence from stored high-confidence signals.
        val rawSignals = db.threatSignalDao().getHighConfidence(minConfidence = 0.3f, limit = 1000)
        val threatMap =
            rawSignals
                .groupBy { signal -> signal.bssid }
                .mapValues { (_, signalList) -> signalList.maxOf { signal -> signal.confidence } }

        // Keep LatLng alongside weight so bounds can be built without re-deriving coordinates.
        data class MapPoint(
            val latLng: LatLng,
            val weight: Double,
        )

        val points =
            scanResults.mapNotNull { result ->
                val lat = result.latitude ?: return@mapNotNull null
                val lng = result.longitude ?: return@mapNotNull null
                // Threat BSSIDs weighted by confidence (0.3–1.0); clean BSSIDs at 0.1.
                val weight = (threatMap[result.bssid] ?: 0.1f).toDouble()
                MapPoint(LatLng(lat, lng), weight)
            }

        if (points.isEmpty()) {
            if (!isDestroyed) {
                runOnUiThread {
                    Toast.makeText(this, "Scan results have no GPS coordinates.", Toast.LENGTH_LONG).show()
                }
            }
            return
        }

        val weightedPoints = points.map { p -> WeightedLatLng(p.latLng, p.weight) }

        val heatmapBuilder = HeatmapTileProvider.Builder()
        heatmapBuilder.weightedData(weightedPoints)
        heatmapBuilder.radius(50)
        val provider = heatmapBuilder.build()

        val boundsBuilder = LatLngBounds.Builder()
        points.forEach { p -> boundsBuilder.include(p.latLng) }
        val bounds = boundsBuilder.build()

        if (!isDestroyed) {
            runOnUiThread {
                map.addTileOverlay(TileOverlayOptions().tileProvider(provider))
                try {
                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 64))
                } catch (e: Exception) {
                    Log.w(TAG, "Camera bounds fit failed — map may be too small", e)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // shutdownNow() interrupts any in-flight task; isDestroyed guards prevent
        // runOnUiThread calls from reaching a destroyed activity.
        executor.shutdownNow()
    }

    companion object {
        private const val TAG = "ScanMapActivity"
        private const val FLOOR_UPDATE_INTERVAL_MS = 1000L
    }
}
