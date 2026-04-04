package com.wscanplus.app.sensor

/**
 * Relative floor estimate derived from barometric pressure change.
 *
 * All values are relative to the calibration baseline — there is no absolute MSL.
 *
 * @param relativeFloor Estimated floor offset from baseline (0 = calibration level, +1 = one floor up).
 * @param deltaHpa      Pressure change from baseline in hPa (positive = higher altitude, computed as baselineHpa − currentHpa).
 * @param confidenceMeters Estimated vertical uncertainty in meters (~±1.5 m typical indoors).
 */
data class FloorEstimate(
    val relativeFloor: Int,
    val deltaHpa: Float,
    val confidenceMeters: Float,
)
