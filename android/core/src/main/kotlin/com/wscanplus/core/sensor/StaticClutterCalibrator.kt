package com.wscanplus.core.sensor

import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

/**
 * Adaptive clutter map for 24GHz FMCW radar.
 *
 * During a calibration pass (~5 s of empty-room frames) the calibrator builds a 150 mm grid of
 * stationary reflections (furniture, walls). After calibration, targets that fall inside a
 * high-hit cell AND move slower than 0.15 m/s are suppressed as environmental noise.
 *
 * Usage:
 *   startCalibration() → feed frames via processAndFilter() → stopCalibration() / auto-stops
 *   After calibration: call processAndFilter() to filter live frames.
 */
class StaticClutterCalibrator {
    private val cellMm = 150.0
    private val clutterMap = ConcurrentHashMap<Pair<Int, Int>, ClutterCell>()

    private var calibrating = false
    private var framesCollected = 0
    private val targetFrames = 80 // ~4–5 s at typical radar frame rate

    private data class ClutterCell(
        val gridX: Int,
        val gridY: Int,
        var centerX: Double,
        var centerY: Double,
        var hits: Int,
        var varianceX: Double = 0.0,
        var varianceY: Double = 0.0,
        var avgResolution: Double = 0.0,
        // Decay factor — approaches 0.5 as the cell is repeatedly read during live filtering
        var persistenceFactor: Double = 1.0,
    )

    fun startCalibration() {
        clutterMap.clear()
        framesCollected = 0
        calibrating = true
    }

    fun stopCalibration() {
        calibrating = false
    }

    fun isCalibrating(): Boolean = calibrating

    fun calibrationProgress(): Float =
        if (!calibrating) {
            0f
        } else {
            (framesCollected.toFloat() / targetFrames).coerceIn(0f, 1f)
        }

    val clutterPointCount: Int get() = clutterMap.size

    val clutterPoints: List<Pair<Double, Double>>
        get() = clutterMap.values.map { it.centerX to it.centerY }

    /**
     * Call once per decoded radar frame.
     * During calibration: records the target and returns false (suppress all output).
     * After calibration: returns true if the target is a genuine moving object, false if clutter.
     */
    fun processAndFilter(target: Ld2450Decoder.Target): Boolean {
        val key = gridKey(target.xMm, target.yMm)

        if (calibrating) {
            record(key, target)
            return false
        }

        val cell = clutterMap[key] ?: return true

        // Suppress slow targets that overlap a well-established clutter cell
        if (target.speedMps < 0.15) {
            val hitRatio = cell.hits.toDouble() / framesCollected.coerceAtLeast(1)
            if (hitRatio > 0.12 || cell.hits > 10) {
                cell.persistenceFactor = (cell.persistenceFactor - 0.001).coerceAtLeast(0.5)
                // Suppress if still strongly persistent
                if (cell.persistenceFactor > 0.6) return false
            }
        }
        return true
    }

    /** Advance internal frame counter; auto-stops calibration when target frame count is reached. */
    fun advanceFrame() {
        if (!calibrating) return
        framesCollected++
        if (framesCollected >= targetFrames) calibrating = false
    }

    private fun gridKey(
        xMm: Double,
        yMm: Double,
    ): Pair<Int, Int> = (xMm / cellMm).toInt() to (yMm / cellMm).toInt()

    private fun record(
        key: Pair<Int, Int>,
        target: Ld2450Decoder.Target,
    ) {
        val cell =
            clutterMap.getOrPut(key) {
                ClutterCell(
                    gridX = key.first,
                    gridY = key.second,
                    centerX = target.xMm,
                    centerY = target.yMm,
                    hits = 0,
                    avgResolution = target.resolutionMm,
                )
            }
        cell.hits++

        // Welford online mean for incremental centroid tracking
        val w = 1.0 / cell.hits
        val dX = target.xMm - cell.centerX
        val newCX = cell.centerX + dX * w
        cell.varianceX += dX * (target.xMm - newCX)
        cell.centerX = newCX

        val dY = target.yMm - cell.centerY
        val newCY = cell.centerY + dY * w
        cell.varianceY += dY * (target.yMm - newCY)
        cell.centerY = newCY

        cell.avgResolution += (target.resolutionMm - cell.avgResolution) * w
    }

    @Suppress("unused")
    private fun distance(
        x1: Double,
        y1: Double,
        x2: Double,
        y2: Double,
    ): Double {
        val dx = x1 - x2
        val dy = y1 - y2
        return sqrt(dx * dx + dy * dy)
    }
}
