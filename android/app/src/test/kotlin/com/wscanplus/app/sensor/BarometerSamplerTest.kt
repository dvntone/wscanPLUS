package com.wscanplus.app.sensor

import com.wscanplus.app.sensor.BarometerSampler.Companion.CONFIDENCE_METERS
import com.wscanplus.app.sensor.BarometerSampler.Companion.FLOOR_HEIGHT_METERS
import com.wscanplus.app.sensor.BarometerSampler.Companion.METERS_PER_HPA
import com.wscanplus.app.sensor.BarometerSampler.Companion.computeFloorEstimate
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.roundToInt

class BarometerSamplerTest {
    @Test
    fun `floor 0 when at baseline`() {
        val estimate = computeFloorEstimate(currentHpa = 1013.25f, baselineHpa = 1013.25f)
        assertEquals(0, estimate.relativeFloor)
        assertEquals(0f, estimate.deltaHpa, 0.001f)
    }

    @Test
    fun `positive floor when above baseline`() {
        // One floor up ≈ 3 m ≈ 0.353 hPa drop in current reading
        val oneFlorHpa = FLOOR_HEIGHT_METERS / METERS_PER_HPA
        val estimate =
            computeFloorEstimate(
                currentHpa = 1013.25f - oneFlorHpa,
                baselineHpa = 1013.25f,
            )
        assertEquals(1, estimate.relativeFloor)
    }

    @Test
    fun `negative floor when below baseline`() {
        val oneFlorHpa = FLOOR_HEIGHT_METERS / METERS_PER_HPA
        val estimate =
            computeFloorEstimate(
                currentHpa = 1013.25f + oneFlorHpa,
                baselineHpa = 1013.25f,
            )
        assertEquals(-1, estimate.relativeFloor)
    }

    @Test
    fun `three floors up`() {
        val threeFloorHpa = (3 * FLOOR_HEIGHT_METERS) / METERS_PER_HPA
        val estimate =
            computeFloorEstimate(
                currentHpa = 1013.25f - threeFloorHpa,
                baselineHpa = 1013.25f,
            )
        assertEquals(3, estimate.relativeFloor)
    }

    @Test
    fun `deltaHpa reflects pressure change direction`() {
        // Current lower than baseline → positive delta (ascended)
        val estimate = computeFloorEstimate(currentHpa = 1010f, baselineHpa = 1013.25f)
        assertEquals(1013.25f - 1010f, estimate.deltaHpa, 0.001f)
    }

    @Test
    fun `confidenceMeters is positive and non-zero`() {
        val estimate = computeFloorEstimate(currentHpa = 1013.25f, baselineHpa = 1013.25f)
        assert(estimate.confidenceMeters > 0f)
    }

    @Test
    fun `large pressure drop maps to many floors`() {
        // 10 hPa drop ≈ 85 m ≈ 28 floors
        val expectedFloors = ((10f * METERS_PER_HPA) / FLOOR_HEIGHT_METERS).roundToInt()
        val estimate =
            computeFloorEstimate(
                currentHpa = 1013.25f - 10f,
                baselineHpa = 1013.25f,
            )
        assertEquals(expectedFloors, estimate.relativeFloor)
    }

    @Test
    fun `constants are physically reasonable`() {
        assert(METERS_PER_HPA in 7f..10f) { "Expected 7-10 m/hPa, got $METERS_PER_HPA" }
        assert(FLOOR_HEIGHT_METERS in 2.5f..4f) { "Expected 2.5-4 m/floor, got $FLOOR_HEIGHT_METERS" }
        assert(CONFIDENCE_METERS in 0.5f..3f) { "Expected 0.5-3 m confidence, got $CONFIDENCE_METERS" }
    }
}
