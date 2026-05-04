package com.wscanplus.app.spatial

interface SpatialMapRenderer {
    fun renderObservations(observations: List<SpatialObservation>)

    fun renderThreatMarkers(markers: List<ThreatMarker>)

    fun renderBaselineArea(area: BaselineArea?)

    fun setFailureMode(reason: String)
}
