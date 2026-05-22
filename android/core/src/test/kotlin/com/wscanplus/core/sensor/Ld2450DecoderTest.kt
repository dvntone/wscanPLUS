package com.wscanplus.core.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Ld2450DecoderTest {
    // MSB=1 → positive: raw = value + 0x8000
    // MSB=0 → negative: raw = -value (magnitude, no sign bit)
    private fun encodeSignedMag(value: Int): Pair<Byte, Byte> {
        val raw: Int = if (value >= 0) value + 0x8000 else -value
        return (raw and 0xFF).toByte() to ((raw shr 8) and 0xFF).toByte()
    }

    private fun encodeUnsigned16(value: Int): Pair<Byte, Byte> =
        (value and 0xFF).toByte() to ((value shr 8) and 0xFF).toByte()

    private fun targetBytes(
        x: Int,
        y: Int,
        speed: Int = 0,
        resolution: Int = 200,
    ): ByteArray {
        val (xl, xh) = encodeSignedMag(x)
        val (yl, yh) = encodeSignedMag(y)
        val (sl, sh) = encodeSignedMag(speed)
        val (rl, rh) = encodeUnsigned16(resolution)
        return byteArrayOf(xl, xh, yl, yh, sl, sh, rl, rh)
    }

    private val dataHeader = byteArrayOf(0xAA.toByte(), 0xFF.toByte(), 0x03.toByte(), 0x00.toByte())
    private val frameTail = byteArrayOf(0x55.toByte(), 0xCC.toByte())
    private val inactiveSlot = ByteArray(8)

    // Builds a valid 30-byte data frame with one active target and two inactive slots.
    private fun buildFrame(
        x: Int,
        y: Int,
        speed: Int = 0,
        resolution: Int = 200,
    ): ByteArray = dataHeader + targetBytes(x, y, speed, resolution) + inactiveSlot + inactiveSlot + frameTail

    @Test
    fun `data frame decodes single target position`() {
        val frame = buildFrame(x = 1500, y = 2000)
        val targets = Ld2450Decoder.decodePacket(frame)
        assertEquals(1, targets.size)
        assertEquals(1500.0, targets[0].xMm, 0.01)
        assertEquals(2000.0, targets[0].yMm, 0.01)
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
    fun `speed converted from cm per second to m per second`() {
        // 200 cm/s → 2.0 m/s
        val frame = buildFrame(x = 500, y = 500, speed = 200)
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
        val frame =
            dataHeader +
                targetBytes(100, 200) +
                targetBytes(300, 400) +
                targetBytes(500, 600) +
                frameTail
        val targets = Ld2450Decoder.decodePacket(frame)
        assertEquals(3, targets.size)
        assertEquals(100.0, targets[0].xMm, 0.01)
        assertEquals(300.0, targets[1].xMm, 0.01)
        assertEquals(500.0, targets[2].xMm, 0.01)
    }

    @Test
    fun `golden frame from HLK-LD2450 v1-03 spec page 12`() {
        // Verbatim example from official spec Table 10:
        // AA FF 03 00 | 0E 03 B1 86 10 00 40 01 | 00*8 | 00*8 | 55 CC
        // Target 1: X=−782 mm, Y=+1713 mm, speed=−16 cm/s (−0.16 m/s), resolution=320 mm
        val frame =
            byteArrayOf(
                0xAA.toByte(),
                0xFF.toByte(),
                0x03.toByte(),
                0x00.toByte(),
                0x0E.toByte(),
                0x03.toByte(),
                0xB1.toByte(),
                0x86.toByte(),
                0x10.toByte(),
                0x00.toByte(),
                0x40.toByte(),
                0x01.toByte(),
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x55.toByte(),
                0xCC.toByte(),
            )
        val targets = Ld2450Decoder.decodePacket(frame)
        assertEquals(1, targets.size)
        assertEquals(-782.0, targets[0].xMm, 0.01)
        assertEquals(1713.0, targets[0].yMm, 0.01)
        assertEquals(-0.16, targets[0].speedMps, 0.001)
        assertEquals(320, targets[0].resolutionMm)
    }

    @Test
    fun `config frame header is not decoded as data`() {
        // Config/ACK frames (FD FC FB FA … 04 03 02 01) must be ignored.
        val configFrame =
            byteArrayOf(
                0xFD.toByte(),
                0xFC.toByte(),
                0xFB.toByte(),
                0xFA.toByte(),
                *ByteArray(22),
                0x04.toByte(),
                0x03.toByte(),
                0x02.toByte(),
                0x01.toByte(),
            )
        val targets = Ld2450Decoder.decodePacket(configFrame)
        assertTrue("Config/ACK frame must not be decoded as data", targets.isEmpty())
    }
}
