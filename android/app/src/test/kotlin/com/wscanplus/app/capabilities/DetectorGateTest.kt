package com.wscanplus.app.capabilities

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectorGateTest {
    private fun manifest(
        wifiScan: Boolean = false,
        wifiRtt: Boolean = false,
        wifiRttAvailableNow: Boolean = false,
        uwb: Boolean = false,
        barometer: Boolean = false,
        cameraIr: CameraIrStatus = CameraIrStatus.UNTESTED,
        acoustic: AcousticStatus = AcousticStatus.UNTESTED,
        bluetoothLe: Boolean = false,
        nsd: Boolean = false,
    ) =
        DeviceCapabilityManifest(
            deviceModel = "Test",
            wifiScan = wifiScan,
            wifiRtt = wifiRtt,
            wifiAware = false,
            uwb = uwb,
            barometer = barometer,
            nsd = nsd,
            bluetoothLe = bluetoothLe,
            proximity = false,
            magnetometer = false,
            accelerometer = false,
            gyroscope = false,
            wifiRttAvailableNow = wifiRttAvailableNow,
            cameraIrCapable = cameraIr,
            acousticSonarCapable = acoustic,
        )

    @Test
    fun wifiScan_requires_wifiScan_true() {
        assertFalse(
            DetectorGate.canRun(
                manifest(wifiScan = false),
                DetectorGate.DetectorRequirement.WIFI_SCAN,
            ),
        )
        assertTrue(
            DetectorGate.canRun(
                manifest(wifiScan = true),
                DetectorGate.DetectorRequirement.WIFI_SCAN,
            ),
        )
    }

    @Test
    fun wifiRtt_requires_both_rtt_flags() {
        assertFalse(
            DetectorGate.canRun(
                manifest(
                    wifiRtt = true,
                    wifiRttAvailableNow = false,
                ),
                DetectorGate.DetectorRequirement.WIFI_RTT,
            ),
        )
        assertFalse(
            DetectorGate.canRun(
                manifest(
                    wifiRtt = false,
                    wifiRttAvailableNow = true,
                ),
                DetectorGate.DetectorRequirement.WIFI_RTT,
            ),
        )
        assertTrue(
            DetectorGate.canRun(
                manifest(
                    wifiRtt = true,
                    wifiRttAvailableNow = true,
                ),
                DetectorGate.DetectorRequirement.WIFI_RTT,
            ),
        )
    }

    @Test
    fun uwbRanging_requires_uwb() {
        assertFalse(
            DetectorGate.canRun(
                manifest(uwb = false),
                DetectorGate.DetectorRequirement.UWB_RANGING,
            ),
        )
        assertTrue(
            DetectorGate.canRun(
                manifest(uwb = true),
                DetectorGate.DetectorRequirement.UWB_RANGING,
            ),
        )
    }

    @Test
    fun floorInference_requires_barometer() {
        assertFalse(
            DetectorGate.canRun(
                manifest(barometer = false),
                DetectorGate.DetectorRequirement.FLOOR_INFERENCE,
            ),
        )
        assertTrue(
            DetectorGate.canRun(
                manifest(barometer = true),
                DetectorGate.DetectorRequirement.FLOOR_INFERENCE,
            ),
        )
    }

    @Test
    fun cameraIrSweep_capable_and_partial_allowed() {
        assertFalse(
            DetectorGate.canRun(
                manifest(cameraIr = CameraIrStatus.UNTESTED),
                DetectorGate.DetectorRequirement.CAMERA_IR_SWEEP,
            ),
        )
        assertFalse(
            DetectorGate.canRun(
                manifest(cameraIr = CameraIrStatus.NOT_CAPABLE),
                DetectorGate.DetectorRequirement.CAMERA_IR_SWEEP,
            ),
        )
        assertTrue(
            DetectorGate.canRun(
                manifest(cameraIr = CameraIrStatus.CAPABLE),
                DetectorGate.DetectorRequirement.CAMERA_IR_SWEEP,
            ),
        )
        assertTrue(
            DetectorGate.canRun(
                manifest(cameraIr = CameraIrStatus.PARTIAL),
                DetectorGate.DetectorRequirement.CAMERA_IR_SWEEP,
            ),
        )
    }

    @Test
    fun glintSweep_same_as_cameraIr() {
        assertFalse(
            DetectorGate.canRun(
                manifest(cameraIr = CameraIrStatus.UNTESTED),
                DetectorGate.DetectorRequirement.GLINT_SWEEP,
            ),
        )
        assertTrue(
            DetectorGate.canRun(
                manifest(cameraIr = CameraIrStatus.CAPABLE),
                DetectorGate.DetectorRequirement.GLINT_SWEEP,
            ),
        )
        assertTrue(
            DetectorGate.canRun(
                manifest(cameraIr = CameraIrStatus.PARTIAL),
                DetectorGate.DetectorRequirement.GLINT_SWEEP,
            ),
        )
    }

    @Test
    fun acousticPresence_requires_capable_only() {
        assertFalse(
            DetectorGate.canRun(
                manifest(acoustic = AcousticStatus.UNTESTED),
                DetectorGate.DetectorRequirement.ACOUSTIC_PRESENCE,
            ),
        )
        assertFalse(
            DetectorGate.canRun(
                manifest(acoustic = AcousticStatus.NOT_CAPABLE),
                DetectorGate.DetectorRequirement.ACOUSTIC_PRESENCE,
            ),
        )
        assertFalse(
            DetectorGate.canRun(
                manifest(acoustic = AcousticStatus.DEVICE_VARIABLE),
                DetectorGate.DetectorRequirement.ACOUSTIC_PRESENCE,
            ),
        )
        assertTrue(
            DetectorGate.canRun(
                manifest(acoustic = AcousticStatus.CAPABLE),
                DetectorGate.DetectorRequirement.ACOUSTIC_PRESENCE,
            ),
        )
    }

    @Test
    fun bleScan_requires_bluetoothLe() {
        assertFalse(
            DetectorGate.canRun(
                manifest(bluetoothLe = false),
                DetectorGate.DetectorRequirement.BLE_SCAN,
            ),
        )
        assertTrue(
            DetectorGate.canRun(
                manifest(bluetoothLe = true),
                DetectorGate.DetectorRequirement.BLE_SCAN,
            ),
        )
    }

    @Test
    fun nsdDiscovery_requires_nsd() {
        assertFalse(
            DetectorGate.canRun(
                manifest(nsd = false),
                DetectorGate.DetectorRequirement.NSD_DISCOVERY,
            ),
        )
        assertTrue(
            DetectorGate.canRun(
                manifest(nsd = true),
                DetectorGate.DetectorRequirement.NSD_DISCOVERY,
            ),
        )
    }
}
