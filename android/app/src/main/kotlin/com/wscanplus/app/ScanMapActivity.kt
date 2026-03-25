package com.wscanplus.app

import android.os.Bundle
import android.util.Log
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
import com.wscanplus.core.db.WscanDatabase
import java.util.concurrent.Executors

/**
 * Displays a GPS-tagged scan history heatmap using Google Maps.
 *
 * Each point corresponds to a scan result with a recorded GPS location.
 * Points are weighted by the maximum threat signal confidence for that BSSID — threat
 * detections appear as hot zones; clean BSSIDs contribute minimal baseline heat.
 *
 * Requires GOOGLE_MAPS_API_KEY in local.properties (injected via secrets-gradle-plugin).
 */
class ScanMapActivity :
    FragmentActivity(),
    OnMapReadyCallback {
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // SupportMapFragment fills the entire content view — no XML layout needed.
        val mapFragment = SupportMapFragment.newInstance()
        supportFragmentManager
            .beginTransaction()
            .replace(android.R.id.content, mapFragment)
            .commit()
        mapFragment.getMapAsync(this)
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
        val db = WscanDatabase.getInstance(applicationContext)
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
    }
}
