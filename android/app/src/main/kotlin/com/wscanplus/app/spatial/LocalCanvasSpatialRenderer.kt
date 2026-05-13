package com.wscanplus.app.spatial

import com.wscanplus.app.LocalHeatmapView

class LocalCanvasSpatialRenderer(
    private val heatmapView: LocalHeatmapView,
    private val status: (title: String, body: String) -> Unit,
) : SpatialMapRenderer {
    override fun renderObservations(observations: List<SpatialObservation>) {
        val points =
            observations.mapNotNull { obs ->
                val lat = obs.latitude ?: return@mapNotNull null
                val lng = obs.longitude ?: return@mapNotNull null
                val weight =
                    obs.rssiDbm?.let { rssi ->
                        ((rssi + 100).coerceIn(0, 70) / 70.0).coerceIn(0.1, 1.0)
                    } ?: 0.1
                LocalHeatmapView.Point(lat, lng, weight)
            }
        if (points.isEmpty()) {
            heatmapView.clearPoints()
            status(
                "No coordinates available",
                "No observations include latitude/longitude. Showing non-map view is recommended.",
            )
            return
        }

        heatmapView.setPoints(points)
        if (!heatmapView.isAttachedToWindow) {
            status(
                "Canvas not displayed",
                "Heatmap view is not attached to the window after loading ${points.size} observations. " +
                    "The view may not be part of the active layout.",
            )
            return
        }
        status(
            "Local canvas — no tiles needed",
            "${points.size} observations rendered. This view uses an offline canvas; " +
                "no map tiles or network connection are required.",
        )
    }

    override fun renderThreatMarkers(markers: List<ThreatMarker>) {
        // Planning-only: threat markers will be blended into weights in a future implementation.
    }

    override fun renderBaselineArea(area: BaselineArea?) {
        // Planning-only: baseline outlines will be added to the canvas later.
    }

    override fun setFailureMode(reason: String) {
        heatmapView.clearPoints()
        status(
            "Map renderer unavailable",
            reason,
        )
    }
}
