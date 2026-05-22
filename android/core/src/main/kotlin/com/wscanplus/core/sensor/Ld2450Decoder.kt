package com.wscanplus.core.sensor

import android.util.Log
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Decodes raw byte packets from the HLK-LD2450 24GHz FMCW radar over BLE or UART.
 *
 * Data frame format (HLK-LD2450 serial protocol v1.03, Table 9):
 *   Header  : AA FF 03 00       (4 bytes)
 *   Target 1: X-lo X-hi Y-lo Y-hi V-lo V-hi RES-lo RES-hi (8 bytes)
 *   Target 2: same layout       (8 bytes)
 *   Target 3: same layout       (8 bytes)
 *   Tail    : 55 CC             (2 bytes)
 *   Total   : 30 bytes, 10 Hz
 *
 * Config/ACK frames use a different header (FD FC FB FA … 04 03 02 01) and are
 * ignored by this decoder — only data frames are parsed.
 *
 * Coordinate encoding (signed-magnitude — NOT two's-complement, per Table 10):
 *   MSB of high byte = 1 → positive: value = raw − 0x8000
 *   MSB of high byte = 0 → negative: value = −raw
 *
 * Speed field is in cm/s.  Resolution field is uint16 mm (distance-gate size).
 */
object Ld2450Decoder {
    private const val TAG = "Ld2450Decoder"
    private const val FRAME_LENGTH = 30
    private const val HEADER_LENGTH = 4
    private const val BYTES_PER_TARGET = 8
    private const val TARGET_COUNT = 3

    private const val MM_PER_FOOT = 304.8

    private val HEADER =
        byteArrayOf(0xAA.toByte(), 0xFF.toByte(), 0x03.toByte(), 0x00.toByte())

    /** Decoded spatial target with millimetre precision. */
    data class Target(
        val id: Int,
        val xMm: Double,
        val yMm: Double,
        val zMm: Double,
        val speedMps: Double,
        val resolutionMm: Int,
    ) {
        val xM: Double get() = xMm / 1_000.0
        val yM: Double get() = yMm / 1_000.0
        val zM: Double get() = zMm / 1_000.0
        val xFt: Double get() = xMm / MM_PER_FOOT
        val yFt: Double get() = yMm / MM_PER_FOOT
        val zFt: Double get() = zMm / MM_PER_FOOT
        val rangeMm: Double get() = sqrt(xMm * xMm + yMm * yMm)
        val rangeM: Double get() = rangeMm / 1_000.0
        val rangeFt: Double get() = rangeMm / MM_PER_FOOT
    }

    /** Physical sensor placement parameters for 3-D coordinate correction. */
    data class SensorCalibration(
        val heightMm: Double = 1200.0,
        val tiltDegrees: Double = 0.0,
        val rotationDegrees: Double = 0.0,
        val xOffsetMm: Double = 0.0,
        val yOffsetMm: Double = 0.0,
    )

    /**
     * Parses a raw BLE notification or UART byte buffer.
     * Returns decoded active targets; inactive slots (x=0, y=0) are omitted.
     * Config/ACK frames (FD FC FB FA header) are skipped automatically.
     */
    fun decodePacket(
        payload: ByteArray,
        calibration: SensorCalibration = SensorCalibration(),
    ): List<Target> {
        if (payload.size < FRAME_LENGTH) return emptyList()

        val headerOffset = findHeader(payload)
        if (headerOffset == -1 || payload.size - headerOffset < FRAME_LENGTH) return emptyList()

        val dataStart = headerOffset + HEADER_LENGTH
        return (0 until TARGET_COUNT).mapNotNull { i ->
            decodeTarget(payload, dataStart + i * BYTES_PER_TARGET, i + 1, calibration)
        }
    }

    private fun findHeader(payload: ByteArray): Int {
        for (i in 0..payload.size - HEADER_LENGTH) {
            if (HEADER.indices.all { payload[i + it] == HEADER[it] }) return i
        }
        return -1
    }

    /**
     * Decodes one 8-byte target record (Table 10):
     *   [0-1] X coordinate (signed-magnitude, mm)
     *   [2-3] Y coordinate (signed-magnitude, mm)
     *   [4-5] Speed        (signed-magnitude, cm/s)
     *   [6-7] Resolution   (uint16, mm — distance-gate size)
     */
    private fun decodeTarget(
        payload: ByteArray,
        offset: Int,
        id: Int,
        cal: SensorCalibration,
    ): Target? {
        if (offset + BYTES_PER_TARGET > payload.size) return null
        return try {
            val xRaw = signedMagnitude(payload[offset], payload[offset + 1])
            val yRaw = signedMagnitude(payload[offset + 2], payload[offset + 3])
            val speedCms = signedMagnitude(payload[offset + 4], payload[offset + 5])
            val resolution = unsigned16(payload[offset + 6], payload[offset + 7])

            // Inactive slot: sensor reports all-zero bytes for absent targets
            if (xRaw == 0 && yRaw == 0) return null

            val xMm = xRaw.toDouble()
            val yMm = yRaw.toDouble()

            val radTilt = Math.toRadians(cal.tiltDegrees)
            val radRot = Math.toRadians(cal.rotationDegrees)

            val yTilted = yMm * cos(radTilt)
            val zTilted = yMm * sin(radTilt)

            val xFinal = xMm * cos(radRot) - yTilted * sin(radRot) + cal.xOffsetMm
            val yFinal = xMm * sin(radRot) + yTilted * cos(radRot) + cal.yOffsetMm
            val zFinal = cal.heightMm + zTilted

            Target(
                id = id,
                xMm = xFinal,
                yMm = yFinal,
                zMm = zFinal,
                speedMps = speedCms / 100.0,
                resolutionMm = resolution,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding target $id at offset $offset: ${e.message}")
            null
        }
    }

    /**
     * Signed-magnitude decode per HLK-LD2450 v1.03 spec:
     *   MSB (bit 15) = 1 → positive: value = raw − 0x8000
     *   MSB (bit 15) = 0 → negative: value = −raw
     */
    private fun signedMagnitude(
        low: Byte,
        high: Byte,
    ): Int {
        val lo = low.toInt() and 0xFF
        val hi = high.toInt() and 0xFF
        val raw = lo or (hi shl 8)
        return if ((hi and 0x80) != 0) raw - 0x8000 else -raw
    }

    private fun unsigned16(
        low: Byte,
        high: Byte,
    ): Int = (low.toInt() and 0xFF) or ((high.toInt() and 0xFF) shl 8)
}
