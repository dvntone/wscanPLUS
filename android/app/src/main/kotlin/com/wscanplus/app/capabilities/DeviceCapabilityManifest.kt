package com.wscanplus.app.capabilities

data class DeviceCapabilityManifest(
    val deviceModel: String,
    val wifiScan: Boolean,
    val wifiRtt: Boolean,
    val wifiAware: Boolean,
    val uwb: Boolean,
    val barometer: Boolean,
    val nsd: Boolean,
    val bluetoothLe: Boolean,
    val proximity: Boolean,
    val magnetometer: Boolean,
    val accelerometer: Boolean,
    val gyroscope: Boolean,
    val wifiRttAvailableNow: Boolean,
    val cameraIrCapable: CameraIrStatus,
    val acousticSonarCapable: AcousticStatus,
)

enum class CameraIrStatus {
    UNTESTED,
    CAPABLE,
    NOT_CAPABLE,
    PARTIAL,
}

enum class AcousticStatus {
    UNTESTED,
    CAPABLE,
    NOT_CAPABLE,
    DEVICE_VARIABLE,
}
