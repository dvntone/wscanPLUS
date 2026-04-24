package com.wscanplus.app

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.location.LocationServices
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

class ScanMapActivity :
    FragmentActivity(),
    OnMapReadyCallback {
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    private var watchdogService: WatchdogService? = null
    private var floorBadge: TextView? = null
    private var statusPanel: View? = null
    private var statusTitle: TextView? = null
    private var statusBody: TextView? = null
    private var fabCenterOnMe: ImageButton? = null
    private var googleMap: GoogleMap? = null
    private var isServiceBound = false
    private val dismissStatusRunnable = Runnable { statusPanel?.visibility = View.GONE }

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
        fabCenterOnMe = findViewById(R.id.fab_center_on_me)
        fabCenterOnMe?.setOnClickListener { centerOnMe() }
        setMapStatus("Loading scan map", "Preparing Google Maps and encrypted scan database.")

        val mapFragment = SupportMapFragment.newInstance()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.map_container, mapFragment)
            .commit()
        mapFragment.getMapAsync(this)
    }

    override fun onStart() {
        super.onStart()
        val bindIntent = Intent(this, WatchdogService::class.java)
        isServiceBound = bindService(bindIntent, serviceConnection, 0)
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(floorUpdateRunnable)
        handler.removeCallbacks(dismissStatusRunnable)
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
        googleMap = map
        setMapStatus("Map ready", "Loading GPS-tagged scan records and heatmap points.")
        executor.execute {
            try {
                loadAndRender(map)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load scan map data", e)
                if (!isDestroyed) {
                    runOnUiThread {
                        setMapStatus(
                            "Map data unavailable",
                            "Failed to load scan map data. Check database state and Google Maps configuration.",
                        )
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
                    setMapStatus(
                        "Encrypted database unavailable",
                        "The scan database could not be opened, so no heatmap can be rendered yet.",
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
                    setMapStatus(
                        "No GPS-tagged scan data",
                        "Run a scan with location enabled. Heatmap points appear after stored results include latitude and longitude.",
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

        data class MapPoint(
            val latLng: LatLng,
            val weight: Double,
        )

        val points =
            scanResults.mapNotNull { result ->
                val lat = result.latitude ?: return@mapNotNull null
                val lng = result.longitude ?: return@mapNotNull null
                val weight = (threatMap[result.bssid] ?: 0.1f).toDouble()
                MapPoint(LatLng(lat, lng), weight)
            }

        if (points.isEmpty()) {
            if (!isDestroyed) {
                runOnUiThread {
                    setMapStatus(
                        "Scan records lack coordinates",
                        "Stored scan rows exist, but none include usable GPS coordinates yet.",
                    )
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
                setMapStatus("Heatmap rendered", "${points.size} GPS-tagged scan points loaded.")
                try {
                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 64))
                    handler.postDelayed(dismissStatusRunnable, MAP_SUCCESS_DISMISS_MS)
                } catch (e: Exception) {
                    Log.w(TAG, "Camera bounds fit failed — map may be too small", e)
                    setMapStatus(
                        "Heatmap rendered",
                        "Points loaded, but automatic camera fit failed. You can still pan and zoom the map.",
                    )
                }
            }
        }
    }

    private fun centerOnMe() {
        val map = googleMap ?: return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Location permission required.", Toast.LENGTH_SHORT).show()
            return
        }
        LocationServices
            .getFusedLocationProviderClient(this)
            .lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 16f),
                    )
                } else {
                    Toast.makeText(this, "Location not yet available.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun setMapStatus(
        title: String,
        body: String,
    ) {
        handler.removeCallbacks(dismissStatusRunnable)
        statusTitle?.text = title
        statusBody?.text = body
        statusPanel?.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(dismissStatusRunnable)
        executor.shutdownNow()
    }

    companion object {
        private const val TAG = "ScanMapActivity"
        private const val FLOOR_UPDATE_INTERVAL_MS = 1000L
        private const val MAP_SUCCESS_DISMISS_MS = 3000L
    }
}
