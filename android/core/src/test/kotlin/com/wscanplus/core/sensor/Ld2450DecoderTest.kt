package com.wscanplus.core.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class Ld2450DecoderTest {
    private fun encodeSignedMag(value: Int): Pair<Byte, Byte> {
        val sign = if (value < 0) 0x80 else 0x00
        val mag = abs(value)
        val low = (mag and 0xFF).toByte()
        val high = ((mag shr 8) and 0x7F or sign).toByte()
        return low to high
    }

    private fun encodeUnsigned16(value: Int): Pair<Byte, Byte> =
        (value and 0xFF).toByte() to ((value shr 8) and 0xFF).toByte()

    private fun buildFrame(
        x: Int,
        y: Int,
        speed: Int = 0,
        resolution: Int = 200,
        header: ByteArray = byteArrayOf(0xFD.toByte(), 0xFC.toByte(), 0xFB.toByte(), 0xFA.toByte()),
    ): ByteArray {
        val (xl, xh) = encodeSignedMag(x)
        val (yl, yh) = encodeSignedMag(y)
        val (sl, sh) = encodeSignedMag(speed)
        val (rl, rh) = encodeUnsigned16(resolution)
        return header + byteArrayOf(xl, xh, yl, yh, sl, sh, rl, rh, 0x00, 0x00) +
            byteArrayOf(0x04, 0x03, 0x02, 0x01)
    }

    @Test
    fun `standard header decodes single target position`() {
        val frame = buildFrame(x = 1500, y = 2000)
        val targets = Ld2450Decoder.decodePacket(frame)
        assertEquals(1, targets.size)
        assertEquals(1500.0, targets[0].xMm, 0.01)
        assertEquals(2000.0, targets[0].yMm, 0.01)
    }

    @Test
    fun `BLE mode header decodes correctly`() {
        val bleHeader = byteArrayOf(0xAA.toByte(), 0xFF.toByte(), 0x03.toByte(), 0x00.toByte())
        val frame = buildFrame(x = 800, y = 1200, header = bleHeader)
        val targets = Ld2450Decoder.decodePacket(frame)
        assertEquals(1, targets.size)
        assertEquals(800.0, targets[0].xMm, 0.01)
    }

    @Test
    fun `negative X coordinate decoded correctly`() {
        val frame = buildFrame(x = -750, y = 1000)
        val targets = Ld2450Decoder.decodePacket(frame)
        assertEquals(1, targets.size)
        assertEquals(-750.0, targets[0].xMm, 0.01)
    }

    @Test
    fun `vacant target x=0 y=0 returns empty list`() {
        val frame = buildFrame(x = 0, y = 0)
        val targets = Ld2450Decoder.decodePacket(frame)
        assertTrue("Vacant target should be filtered out", targets.isEmpty())
    }

    @Test
    fun `speed converted from mm per second to m per second`() {
        // 2000 mm/s = 2.0 m/s; fits in 15-bit signed magnitude (max 32767)
        val frame = buildFrame(x = 500, y = 500, speed = 2000)
        val targets = Ld2450Decoder.decodePacket(frame)
        assertEquals(1, targets.size)
        assertEquals(2.0, targets[0].speedMps, 0.01)
    }

    @Test
    fun `short payload returns empty list`() {
        val targets = Ld2450Decoder.decodePacket(byteArrayOf(0x01, 0x02, 0x03))
        assertTrue(targets.isEmpty())
    }

    @Test
    fun `zero tilt leaves z equal to sensor height`() {
        val cal = Ld2450Decoder.SensorCalibration(heightMm = 1000.0, tiltDegrees = 0.0)
        val frame = buildFrame(x = 1, y = 1000)
        val targets = Ld2450Decoder.decodePacket(frame, cal)
        assertEquals(1, targets.size)
        assertEquals(1000.0, targets[0].zMm, 1.0)
    }

    @Test
    fun `three targets decoded from single frame`() {
        val header = byteArrayOf(0xFD.toByte(), 0xFC.toByte(), 0xFB.toByte(), 0xFA.toByte())

        fun targetBytes(
            x: Int,
            y: Int,
        ): ByteArray {
            val (xl, xh) = encodeSignedMag(x)
            val (yl, yh) = encodeSignedMag(y)
            return byteArrayOf(xl, xh, yl, yh, 0, 0, 0xC8.toByte(), 0, 0, 0)
        }

        val frame =
            header +
                targetBytes(100, 200) +
                targetBytes(300, 400) +
                targetBytes(500, 600) +
                byteArrayOf(0x04, 0x03, 0x02, 0x01)
        val targets = Ld2450Decoder.decodePacket(frame)
        assertEquals(3, targets.size)
        assertEquals(100.0, targets[0].xMm, 0.01)
        assertEquals(300.0, targets[1].xMm, 0.01)
        assertEquals(500.0, targets[2].xMm, 0.01)
    }
}
