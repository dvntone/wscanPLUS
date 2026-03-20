package com.wscanplus.app.location

data class LocationSample(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val altitudeMeters: Double?,
    val speedKph: Float?,
    val capturedAt: Long,
    val provider: String?,
    val isMock: Boolean,
) {
    fun isFresh(nowMillis: Long): Boolean = nowMillis - capturedAt <= MAX_SAMPLE_AGE_MS

    companion object {
        const val MAX_SAMPLE_AGE_MS: Long = 30_000
    }
}
