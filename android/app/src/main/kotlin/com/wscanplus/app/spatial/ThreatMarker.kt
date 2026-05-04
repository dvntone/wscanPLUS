package com.wscanplus.app.spatial

data class ThreatMarker(
    val bssid: String,
    val latitude: Double?,
    val longitude: Double?,
    val confidence: Float,
    val capturedAtEpochMillis: Long,
)
