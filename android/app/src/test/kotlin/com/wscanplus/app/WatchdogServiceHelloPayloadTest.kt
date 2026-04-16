package com.wscanplus.app

import com.wscanplus.app.capabilities.AcousticStatus
import com.wscanplus.app.capabilities.CameraIrStatus
import com.wscanplus.app.capabilities.DeviceCapabilityManifest
import com.wscanplus.app.sensor.FloorEstimate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchdogServiceHelloPayloadTest {
    private val manifest =
        DeviceCapabilityManifest(
            deviceModel = "Pixel Test",
            wifiScan = true,
            wifiRtt = true,
            wifiAware = false,
            uwb = false,
            barometer = true,
            nsd = true,
            bluetoothLe = true,
            proximity = true,
            magnetometer = true,
            accelerometer = true,
            gyroscope = true,
            wifiRttAvailableNow = true,
            cameraIrCapable = CameraIrStatus.UNTESTED,
            acousticSonarCapable = AcousticStatus.UNTESTED,
        )

    @Test
    fun `hello contains type, deviceId, seq, and sentAt`() {
        val sentAt = 1_700_000_000_000L
        val payload =
            buildWatchdogHelloJson(
                deviceId = "serial-123",
                seq = 3,
                sentAt = sentAt,
                capabilityManifest = null,
            )

        val root = Json.parseToJsonElement(payload).jsonObject

        assertEquals("hello", root.getValue("type").jsonPrimitive.content)
        assertEquals("serial-123", root.getValue("deviceId").jsonPrimitive.content)
        assertEquals(
            3,
            root
                .getValue("seq")
                .jsonPrimitive.content
                .toInt(),
        )
        assertEquals(
            sentAt,
            root
                .getValue("sentAt")
                .jsonPrimitive.content
                .toLong(),
        )
    }

    @Test
    fun `hello contains capabilities when manifest is present`() {
        val payload =
            buildWatchdogHelloJson(
                deviceId = "serial-123",
                seq = 0,
                sentAt = 1_700_000_000_000L,
                capabilityManifest = manifest,
            )

        val root = Json.parseToJsonElement(payload).jsonObject

        assertTrue(root.containsKey("capabilities"))
        assertEquals(
            "Pixel Test",
            root
                .getValue("capabilities")
                .jsonObject
                .getValue("deviceModel")
                .jsonPrimitive.content,
        )
    }

    @Test
    fun `hello omits capabilities when manifest is null`() {
        val payload =
            buildWatchdogHelloJson(
                deviceId = "serial-456",
                seq = 0,
                sentAt = 1_700_000_000_000L,
                capabilityManifest = null,
            )

        val root = Json.parseToJsonElement(payload).jsonObject

        assertFalse(root.containsKey("capabilities"))
    }

    @Test
    fun `hello includes floorEstimate when provided`() {
        val estimate =
            FloorEstimate(
                relativeFloor = 2,
                deltaHpa = 0.71f,
                confidenceMeters = 1.5f,
                observedAt = 1_700_000_001_000L,
            )
        val payload =
            buildWatchdogHelloJson(
                deviceId = "serial-789",
                seq = 1,
                sentAt = 1_700_000_000_000L,
                capabilityManifest = null,
                floorEstimate = estimate,
            )

        val root = Json.parseToJsonElement(payload).jsonObject
        assertTrue(root.containsKey("floorEstimate"))
        val fe = root.getValue("floorEstimate").jsonObject
        assertEquals(
            2,
            fe
                .getValue("relativeFloor")
                .jsonPrimitive.content
                .toInt(),
        )
        assertEquals(
            1_700_000_001_000L,
            fe
                .getValue("observedAt")
                .jsonPrimitive.content
                .toLong(),
        )
    }

    @Test
    fun `hello omits floorEstimate when null`() {
        val payload =
            buildWatchdogHelloJson(
                deviceId = "serial-789",
                seq = 0,
                sentAt = 1_700_000_000_000L,
                capabilityManifest = null,
                floorEstimate = null,
            )

        val root = Json.parseToJsonElement(payload).jsonObject
        assertFalse(root.containsKey("floorEstimate"))
    }

    @Test
    fun `seq field is serialized into hello JSON`() {
        val payload = buildWatchdogHelloJson("d", seq = 7, sentAt = 1L, capabilityManifest = null)
        val seq =
            Json
                .parseToJsonElement(payload)
                .jsonObject
                .getValue("seq")
                .jsonPrimitive.content
                .toInt()
        assertEquals(7, seq)
    }
}
