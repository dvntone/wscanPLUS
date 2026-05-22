package com.wscanplus.core.sensor

import android.util.Log
import kotlin.math.cos
import kotlin.math.sin

/**
 * Decodes raw byte packets from the HLK-LD2450 24GHz FMCW radar over BLE or UART.
 * Supports both BLE-mode and standard UART frame headers; tracks up to 3 simultaneous targets.
 */
object Ld2450Decoder {
    private const val TAG = "Ld2450Decoder"

    // Standard UART header: 0xFAFBFCFD little-endian
    private val HEADER_STANDARD = byteArrayOf(0xFD.toByte(), 0xFC.toByte(), 0xFB.toByte(), 0xFA.toByte())

    // BLE notification header
    private val HEADER_BLE = byteArrayOf(0xAA.toByte(), 0xFF.toByte(), 0x03.toByte(), 0x00.toByte())

    /** Decoded spatial target with millimetre precision. */
    data class Target(
        val id: Int,
        val xMm: Double,
        val yMm: Double,
        val zMm: Double,
        val speedMps: Double,
        val resolutionMm: Double,
    )

    /** Physical sensor placement parameters for 3D coordinate correction. */
    data class SensorCalibration(
        val heightMm: Double = 1200.0,
        val tiltDegrees: Double = 0.0,
        val rotationDegrees: Double = 0.0,
        val xOffsetMm: Double = 0.0,
        val yOffsetMm: Double = 0.0,
    )

    /**
     * Parses a raw BLE notification or UART byte buffer.
     * Returns decoded targets; vacant slots (x=0, y=0) are omitted.
     */
    fun decodePacket(
        payload: ByteArray,
        calibration: SensorCalibration = SensorCalibration(),
    ): List<Target> {
        if (payload.size < 10) return emptyList()

        val headerOffset = findHeader(payload)
        if (headerOffset == -1) {
            // No recognised header — try raw aligned parse (BLE characteristic chunks)
            return parseAligned(payload, 0, payload.size, calibration)
        }

        val dataOffset = headerOffset + 4
        val available = payload.size - dataOffset
        if (available < 14) return emptyList() // need at least 1 target (10 B) + trailer (4 B)

        val maxTargets = (available - 4) / 10
        return (0 until minOf(3, maxTargets))
            .mapNotNull { i -> decodeTarget(payload, dataOffset + i * 10, i + 1, calibration) }
    }

    private fun findHeader(payload: ByteArray): Int {
        for (i in 0..payload.size - 4) {
            if (matches(payload, i, HEADER_STANDARD) || matches(payload, i, HEADER_BLE)) return i
        }
        return -1
    }

    private fun matches(
        buf: ByteArray,
        offset: Int,
        header: ByteArray,
    ): Boolean = header.indices.all { buf[offset + it] == header[it] }

    private fun parseAligned(
        payload: ByteArray,
        start: Int,
        length: Int,
        calibration: SensorCalibration,
    ): List<Target> {
        val count = minOf(payload.size - start, length) / 10
        return (0 until minOf(3, count))
            .mapNotNull { i -> decodeTarget(payload, start + i * 10, i + 1, calibration) }
    }

    /**
     * Decodes a single 10-byte target subfield.
     *
     * Byte layout (HLK spec):
     *   [0-1]  X coordinate — signed magnitude, MSB bit 15 = sign
     *   [2-3]  Y coordinate — signed magnitude, MSB bit 15 = sign
     *   [4-5]  Speed — signed magnitude, MSB bit 15 = sign
     *   [6-7]  Resolution / uncertainty radius — unsigned 16-bit
     *   [8-9]  Status / CRC
     */
    private fun decodeTarget(
        payload: ByteArray,
        offset: Int,
        id: Int,
        cal: SensorCalibration,
    ): Target? {
        return try {
            val xRaw = signedMagnitude(payload[offset], payload[offset + 1])
            val yRaw = signedMagnitude(payload[offset + 2], payload[offset + 3])
            val speedRaw = signedMagnitude(payload[offset + 4], payload[offset + 5])
            val resolution = unsigned16(payload[offset + 6], payload[offset + 7]).toDouble()

            // HLK-LD2450 marks vacant target slots with (0, 0)
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
                speedMps = speedRaw / 1000.0, // mm/s → m/s
                resolutionMm = if (resolution <= 0.0) 100.0 else resolution,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding target $id at offset $offset: ${e.message}")
            null
        }
    }

    /** Signed-magnitude decode: bit 15 of the high byte is the sign. */
    private fun signedMagnitude(
        low: Byte,
        high: Byte,
    ): Int {
        val lo = low.toInt() and 0xFF
        val hi = high.toInt() and 0xFF
        val sign = if ((hi and 0x80) != 0) -1 else 1
        val magnitude = lo or ((hi and 0x7F) shl 8)
        return magnitude * sign
    }

    private fun unsigned16(
        low: Byte,
        high: Byte,
    ): Int = (low.toInt() and 0xFF) or ((high.toInt() and 0xFF) shl 8)
}
