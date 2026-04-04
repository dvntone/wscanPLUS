package com.wscanplus.app

import com.wscanplus.app.capabilities.AcousticStatus
import com.wscanplus.app.capabilities.CameraIrStatus
import com.wscanplus.app.capabilities.DeviceCapabilityManifest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchdogServiceHelloPayloadTest {
    @Test
    fun `hello message contains capabilities when manifest is present`() {
        val manifest =
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

        val payload =
            buildWatchdogHelloJson(
                deviceId = "serial-123",
                capabilityManifest = manifest,
            )

        val root =
            Json
                .parseToJsonElement(payload)
                .jsonObject

        assertEquals("hello", root.getValue("type").jsonPrimitive.content)
        assertEquals("serial-123", root.getValue("deviceId").jsonPrimitive.content)
        assertTrue(root.containsKey("capabilities"))
        assertEquals(
            "Pixel Test",
            root
                .getValue("capabilities")
                .jsonObject
                .getValue("deviceModel")
                .jsonPrimitive
                .content,
        )
    }
}
