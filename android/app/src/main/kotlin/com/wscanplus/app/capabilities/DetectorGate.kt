package com.wscanplus.app.capabilities

object DetectorGate {
    enum class DetectorRequirement {
        WIFI_SCAN,
        WIFI_RTT,
        UWB_RANGING,
        FLOOR_INFERENCE,
        CAMERA_IR_SWEEP,
        GLINT_SWEEP,
        ACOUSTIC_PRESENCE,
        BLE_SCAN,
        NSD_DISCOVERY,
    }

    fun canRun(
        manifest: DeviceCapabilityManifest,
        requirement: DetectorRequirement,
    ): Boolean =
        when (requirement) {
            DetectorRequirement.WIFI_SCAN -> manifest.wifiScan
            DetectorRequirement.WIFI_RTT -> manifest.wifiRtt && manifest.wifiRttAvailableNow
            DetectorRequirement.UWB_RANGING -> manifest.uwb
            DetectorRequirement.FLOOR_INFERENCE -> manifest.barometer
            DetectorRequirement.CAMERA_IR_SWEEP ->
                manifest.cameraIrCapable == CameraIrStatus.CAPABLE ||
                    manifest.cameraIrCapable == CameraIrStatus.PARTIAL
            DetectorRequirement.GLINT_SWEEP ->
                manifest.cameraIrCapable == CameraIrStatus.CAPABLE ||
                    manifest.cameraIrCapable == CameraIrStatus.PARTIAL
            DetectorRequirement.ACOUSTIC_PRESENCE ->
                manifest.acousticSonarCapable == AcousticStatus.CAPABLE
            DetectorRequirement.BLE_SCAN -> manifest.bluetoothLe
            DetectorRequirement.NSD_DISCOVERY -> manifest.nsd
        }
}
