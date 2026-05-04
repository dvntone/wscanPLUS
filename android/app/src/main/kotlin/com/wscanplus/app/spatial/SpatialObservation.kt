package com.wscanplus.app.spatial

data class SpatialObservation(
    val bssid: String,
    val ssid: String?,
    val latitude: Double?,
    val longitude: Double?,
    val accuracyMeters: Float?,
    val rssiDbm: Int?,
    val channel: Int?,
    val capturedAtEpochMillis: Long,
    val source: String,
)
