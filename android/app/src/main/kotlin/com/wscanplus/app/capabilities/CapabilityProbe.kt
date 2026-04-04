package com.wscanplus.app.capabilities

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.wifi.rtt.WifiRttManager
import android.os.Build
import androidx.core.content.ContextCompat

object CapabilityProbe {
    suspend fun probe(context: Context): DeviceCapabilityManifest {
        val packageManager = context.packageManager
        val sensorManager = context.getSystemService(SensorManager::class.java)

        val wifiFeature =
            packageManager.hasSystemFeature(
                PackageManager.FEATURE_WIFI,
            )
        val locationGranted = hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val wifiPermissionsGranted =
            hasPermission(context, Manifest.permission.CHANGE_WIFI_STATE) &&
                hasPermission(context, Manifest.permission.ACCESS_WIFI_STATE) &&
                locationGranted
        val wifiRttFeature =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_RTT)
        val wifiRttAvailableNow =
            if (wifiRttFeature && locationGranted) {
                context
                    .getSystemService(WifiRttManager::class.java)
                    ?.isAvailable == true
            } else {
                false
            }
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
                    context
                        .getSystemService(BluetoothManager::class.java)
                        ?.adapter != null,
            proximity = hasSensor(sensorManager, Sensor.TYPE_PROXIMITY),
            magnetometer = hasSensor(sensorManager, Sensor.TYPE_MAGNETIC_FIELD),
            accelerometer = hasSensor(sensorManager, Sensor.TYPE_ACCELEROMETER),
            gyroscope = hasSensor(sensorManager, Sensor.TYPE_GYROSCOPE),
            wifiRttAvailableNow = wifiRttAvailableNow,
            cameraIrCapable = CameraIrStatus.UNTESTED,
            acousticSonarCapable = AcousticStatus.UNTESTED,
        )
    }

    private fun hasPermission(
        context: Context,
        permission: String,
    ): Boolean =
        ContextCompat
            .checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private fun hasSensor(
        sensorManager: SensorManager?,
        sensorType: Int,
    ): Boolean = sensorManager?.getDefaultSensor(sensorType) != null
}
