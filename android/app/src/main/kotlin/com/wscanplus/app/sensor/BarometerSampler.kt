package com.wscanplus.app.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.roundToInt

/**
 * Samples [Sensor.TYPE_PRESSURE] and emits [FloorEstimate] relative to a calibration baseline.
 *
 * Usage:
 * 1. Call [start] to register the sensor listener.
 * 2. Call [calibrate] at the reference floor (e.g. "Ground Floor" or building entry).
 * 3. [onEstimate] is invoked on each new reading while a baseline is set.
 * 4. Call [stop] to unregister.
 *
 * Floor math:
 *   ~1 hPa ≈ 8.5 m altitude change (ISA standard lapse rate, sea-level approximation).
 *   Typical residential floor height ≈ 3 m → ~0.353 hPa per floor.
 *   deltaHpa = baselineHpa − currentHpa, so positive deltaHpa = lower current pressure = higher altitude.
 *
 * This class has no Android framework dependencies beyond [SensorManager] and [SensorEvent],
 * making the floor calculation logic unit-testable without Robolectric.
 */
class BarometerSampler(
    private val sensorManager: SensorManager,
    private val onEstimate: (FloorEstimate) -> Unit,
    /**
     * When true, the first sensor reading automatically becomes the floor-0 baseline.
     * Convenient for service-level usage where calibration is deferred to the first reading.
     */
    val autoCalibrate: Boolean = false,
) : SensorEventListener {
    private var baselineHpa: Float? = null
    private var started = false
    private val pressureSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

    /** True when the device has a barometer. */
    val isAvailable: Boolean get() = pressureSensor != null

    /**
     * Register the sensor listener. No-op if already started or no barometer is present.
     */
    fun start() {
        if (started) return
        val sensor =
            pressureSensor ?: run {
                Log.w(TAG, "No barometer sensor available — BarometerSampler inactive")
                return
            }
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        started = true
        Log.d(TAG, "BarometerSampler started")
    }

    /** Unregister the sensor listener and clear the baseline. */
    fun stop() {
        if (!started) return
        sensorManager.unregisterListener(this)
        baselineHpa = null
        started = false
        Log.d(TAG, "BarometerSampler stopped")
    }

    /**
     * Set the current pressure reading as the floor-0 baseline.
     * The next sensor event after calibration will be delivered as floor 0 with deltaHpa ≈ 0.
     */
    fun calibrate(baselineHpa: Float) {
        this.baselineHpa = baselineHpa
        Log.i(TAG, "Barometer baseline set: $baselineHpa hPa")
    }

    /** Calibrate using the most recent sensor reading. No-op if no reading has arrived yet. */
    fun calibrateNow() {
        val current =
            lastReadingHpa ?: run {
                Log.w(TAG, "calibrateNow() called before any sensor reading arrived")
                return
            }
        calibrate(current)
    }

    private var lastReadingHpa: Float? = null

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_PRESSURE) return
        val hpa = event.values[0]
        lastReadingHpa = hpa
        if (baselineHpa == null && autoCalibrate) {
            baselineHpa = hpa
            Log.i(TAG, "Barometer auto-calibrated on first reading: $hpa hPa")
        }
        val baseline = baselineHpa ?: return
        val estimate = computeFloorEstimate(hpa, baseline)
        onEstimate(estimate)
    }

    override fun onAccuracyChanged(
        sensor: Sensor,
        accuracy: Int,
    ) {
        // Not used.
    }

    companion object {
        private const val TAG = "BarometerSampler"

        /**
         * Meters of altitude change per hPa at sea level (ISA standard).
         * With deltaHpa computed as baselineHpa − currentHpa, a positive hPa delta
         * means lower current pressure and therefore higher altitude (ascended).
         */
        internal const val METERS_PER_HPA = 8.5f

        /** Assumed floor height in meters for residential/office buildings. */
        internal const val FLOOR_HEIGHT_METERS = 3.0f

        /** Estimated vertical uncertainty for consumer barometers indoors (meters). */
        internal const val CONFIDENCE_METERS = 1.5f

        /**
         * Compute a [FloorEstimate] from [currentHpa] relative to [baselineHpa].
         *
         * Exposed as a pure static function so unit tests can exercise the math
         * without constructing a [BarometerSampler].
         */
        fun computeFloorEstimate(
            currentHpa: Float,
            baselineHpa: Float,
        ): FloorEstimate {
            val deltaHpa = baselineHpa - currentHpa
            val deltaMeters = deltaHpa * METERS_PER_HPA
            val relativeFloor = (deltaMeters / FLOOR_HEIGHT_METERS).roundToInt()
            return FloorEstimate(
                relativeFloor = relativeFloor,
                deltaHpa = deltaHpa,
                confidenceMeters = CONFIDENCE_METERS,
            )
        }
    }
}
