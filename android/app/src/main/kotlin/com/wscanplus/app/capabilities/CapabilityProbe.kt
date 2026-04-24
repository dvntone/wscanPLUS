@file:Suppress("ktlint:standard:function-expression-body")

package com.wscanplus.app.capabilities

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.wifi.rtt.WifiRttManager
import android.os.Build
import androidx.core.content.ContextCompat

object CapabilityProbe {
    suspend fun probe(context: Context): DeviceCapabilityManifest {
        val appContext = context.applicationContext
        val packageManager = appContext.packageManager
        val sensorManager = appContext.getSystemService(SensorManager::class.java)

        val wifiFeature = packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI)
        val locationGranted = hasPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION)
        val wifiPermissionsGranted =
            hasPermission(appContext, Manifest.permission.CHANGE_WIFI_STATE) &&
                hasPermission(appContext, Manifest.permission.ACCESS_WIFI_STATE) &&
                locationGranted
        val wifiRttFeature =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_RTT)
        val wifiRttAvailableNow =
            wifiRttFeature &&
                locationGranted &&
                appContext.getSystemService(WifiRttManager::class.java)?.isAvailable == true
        val uwb =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                @Suppress("InlinedApi")
                packageManager.hasSystemFeature(PackageManager.FEATURE_UWB)
            } else {
                false
            }

        return DeviceCapabilityManifest(
            deviceModel = Build.MODEL,
            wifiScan = wifiFeature && wifiPermissionsGranted,
            wifiRtt = wifiRttFeature && locationGranted,
            wifiAware =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE),
            uwb = uwb,
            barometer = hasSensor(sensorManager, Sensor.TYPE_PRESSURE),
            nsd = true,
            bluetoothLe =
                packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) &&
                    appContext.getSystemService(BluetoothManager::class.java)?.adapter != null,
            proximity = hasSensor(sensorManager, Sensor.TYPE_PROXIMITY),
            magnetometer = hasSensor(sensorManager, Sensor.TYPE_MAGNETIC_FIELD),
            accelerometer = hasSensor(sensorManager, Sensor.TYPE_ACCELEROMETER),
            gyroscope = hasSensor(sensorManager, Sensor.TYPE_GYROSCOPE),
            wifiRttAvailableNow = wifiRttAvailableNow,
            cameraIrCapable = probeCameraDepth(appContext, packageManager),
            acousticSonarCapable = probeAcoustic(appContext, packageManager),
        )
    }

    private fun probeCameraDepth(
        context: Context,
        packageManager: PackageManager,
    ): CameraIrStatus {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            return CameraIrStatus.NOT_CAPABLE
        }
        val cameraManager = context.getSystemService(CameraManager::class.java) ?: return CameraIrStatus.UNTESTED
        return try {
            val hasDepthOutput =
                cameraManager.cameraIdList.any { cameraId ->
                    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                    val capabilities =
                        characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: intArrayOf()
                    capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT)
                }
            if (hasDepthOutput) CameraIrStatus.CAPABLE else CameraIrStatus.NOT_CAPABLE
        } catch (_: Exception) {
            CameraIrStatus.UNTESTED
        }
    }

    private fun probeAcoustic(
        context: Context,
        packageManager: PackageManager,
    ): AcousticStatus {
        val hasMicrophone = packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)
        val hasAudioOutput = packageManager.hasSystemFeature(FEATURE_AUDIO_OUTPUT)
        if (!hasMicrophone || !hasAudioOutput) {
            return AcousticStatus.NOT_CAPABLE
        }
        return if (hasPermission(context, Manifest.permission.RECORD_AUDIO)) {
            AcousticStatus.CAPABLE
        } else {
            AcousticStatus.DEVICE_VARIABLE
        }
    }

    private fun hasPermission(
        context: Context,
        permission: String,
    ): Boolean = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private fun hasSensor(
        sensorManager: SensorManager?,
        sensorType: Int,
    ): Boolean = sensorManager?.getDefaultSensor(sensorType) != null

    private const val FEATURE_AUDIO_OUTPUT = "android.hardware.audio.output"
}
