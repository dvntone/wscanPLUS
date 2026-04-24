package com.wscanplus.app.capabilities

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

class CapabilityReadinessSurveyor(
    context: Context,
) : PermissionReadinessProvider {
    private val appContext = context.applicationContext

    override fun current(): PermissionReadiness {
        val packageManager = appContext.packageManager
        val wifiManager = appContext.getSystemService(WifiManager::class.java)
        val bluetoothManager = appContext.getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter = bluetoothManager?.adapter
        val telephonyManager = appContext.getSystemService(TelephonyManager::class.java)
        val sensorManager = appContext.getSystemService(SensorManager::class.java)

        return evaluate(
            ReadinessInputs(
                sdkInt = Build.VERSION.SDK_INT,
                hasFineLocation = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION),
                hasAccessWifiState = hasPermission(Manifest.permission.ACCESS_WIFI_STATE),
                hasChangeWifiState = hasPermission(Manifest.permission.CHANGE_WIFI_STATE),
                hasNearbyWifiDevicesPermission = hasPermission(Manifest.permission.NEARBY_WIFI_DEVICES),
                locationServicesEnabled = DeviceLocationState.isLocationEnabled(appContext),
                hasWifiHardware = packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI),
                hasWifiManager = wifiManager != null,
                hasBleHardware = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE),
                hasBluetoothAdapter = bluetoothAdapter != null,
                bluetoothEnabled = bluetoothAdapter?.isEnabled == true,
                hasBluetoothScanPermission = hasPermission(Manifest.permission.BLUETOOTH_SCAN),
                hasBluetoothConnectPermission = hasPermission(Manifest.permission.BLUETOOTH_CONNECT),
                hasTelephonyHardware = packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY),
                hasTelephonyManager = telephonyManager != null,
                hasAnySupportedSensor = hasAnySupportedSensor(sensorManager),
            ),
        )
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED

    private fun hasAnySupportedSensor(sensorManager: SensorManager?): Boolean =
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null ||
            sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null ||
            sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null ||
            sensorManager?.getDefaultSensor(Sensor.TYPE_PRESSURE) != null ||
            sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null

    data class ReadinessInputs(
        val sdkInt: Int,
        val hasFineLocation: Boolean,
        val hasAccessWifiState: Boolean,
        val hasChangeWifiState: Boolean,
        val hasNearbyWifiDevicesPermission: Boolean,
        val locationServicesEnabled: Boolean,
        val hasWifiHardware: Boolean,
        val hasWifiManager: Boolean,
        val hasBleHardware: Boolean,
        val hasBluetoothAdapter: Boolean,
        val bluetoothEnabled: Boolean,
        val hasBluetoothScanPermission: Boolean,
        val hasBluetoothConnectPermission: Boolean,
        val hasTelephonyHardware: Boolean,
        val hasTelephonyManager: Boolean,
        val hasAnySupportedSensor: Boolean,
    )

    companion object {
        internal fun evaluate(inputs: ReadinessInputs): PermissionReadiness {
            val blockers = mutableSetOf<ReadinessBlocker>()
            val hasWifiSupport = inputs.hasWifiHardware && inputs.hasWifiManager
            val hasBleSupport = inputs.hasBleHardware && inputs.hasBluetoothAdapter
            val legacyBleLocationDisabled =
                inputs.sdkInt < Build.VERSION_CODES.S &&
                    !inputs.locationServicesEnabled
            val hasNearbyWifiDevices =
                inputs.sdkInt < Build.VERSION_CODES.TIRAMISU ||
                    inputs.hasNearbyWifiDevicesPermission

            if (!inputs.hasFineLocation) blockers += ReadinessBlocker.MISSING_FINE_LOCATION
            if (hasWifiSupport && !inputs.hasAccessWifiState) {
                blockers += ReadinessBlocker.MISSING_ACCESS_WIFI_STATE
            }
            if (hasWifiSupport && !inputs.hasChangeWifiState) {
                blockers += ReadinessBlocker.MISSING_CHANGE_WIFI_STATE
            }
            if (!inputs.locationServicesEnabled) {
                blockers += ReadinessBlocker.LOCATION_SERVICES_DISABLED
            }
            if (hasWifiSupport && !hasNearbyWifiDevices) {
                blockers += ReadinessBlocker.MISSING_NEARBY_WIFI_DEVICES
            }

            val missingWifiPrerequisites =
                listOf(
                    !inputs.hasFineLocation,
                    !inputs.hasAccessWifiState,
                    !inputs.hasChangeWifiState,
                    !hasNearbyWifiDevices,
                ).any { it }
            val wifiPermissionMissing = hasWifiSupport && missingWifiPrerequisites

            val wifiState =
                when {
                    !hasWifiSupport -> CapabilityState.UNSUPPORTED_BY_HARDWARE
                    wifiPermissionMissing -> CapabilityState.SUPPORTED_PERMISSION_MISSING
                    !inputs.locationServicesEnabled -> CapabilityState.SUPPORTED_DISABLED_BY_SYSTEM
                    else -> CapabilityState.SUPPORTED_AND_READY
                }

            val bleScanPermissionMissing =
                if (inputs.sdkInt >= Build.VERSION_CODES.S) {
                    !inputs.hasBluetoothScanPermission
                } else {
                    !inputs.hasFineLocation
                }
            val bleConnectPermissionMissing =
                inputs.sdkInt >= Build.VERSION_CODES.S &&
                    !inputs.hasBluetoothConnectPermission
            val modernBleScanPermissionMissing =
                inputs.sdkInt >= Build.VERSION_CODES.S &&
                    bleScanPermissionMissing

            if (hasBleSupport && modernBleScanPermissionMissing) {
                blockers += ReadinessBlocker.MISSING_BLE_SCAN_PERMISSION
            }
            if (hasBleSupport && bleConnectPermissionMissing) {
                blockers += ReadinessBlocker.MISSING_BLE_CONNECT_PERMISSION
            }
            if (hasBleSupport && !inputs.bluetoothEnabled) {
                blockers += ReadinessBlocker.BLUETOOTH_DISABLED
            }

            val bleState =
                when {
                    !hasBleSupport -> CapabilityState.UNSUPPORTED_BY_HARDWARE
                    bleScanPermissionMissing || bleConnectPermissionMissing ->
                        CapabilityState.SUPPORTED_PERMISSION_MISSING
                    legacyBleLocationDisabled || !inputs.bluetoothEnabled ->
                        CapabilityState.SUPPORTED_DISABLED_BY_SYSTEM
                    else -> CapabilityState.SUPPORTED_AND_READY
                }

            val hasCellularSupport = inputs.hasTelephonyHardware && inputs.hasTelephonyManager
            val cellularState =
                when {
                    !hasCellularSupport -> CapabilityState.UNSUPPORTED_BY_HARDWARE
                    !inputs.hasFineLocation -> {
                        blockers += ReadinessBlocker.CELL_PERMISSION_MISSING
                        CapabilityState.SUPPORTED_PERMISSION_MISSING
                    }
                    else -> CapabilityState.SUPPORTED_AND_READY
                }

            val sensorsState =
                if (inputs.hasAnySupportedSensor) {
                    CapabilityState.SUPPORTED_AND_READY
                } else {
                    blockers += ReadinessBlocker.SENSOR_UNAVAILABLE
                    CapabilityState.UNSUPPORTED_BY_HARDWARE
                }

            return PermissionReadiness(
                wifi = wifiState,
                ble = bleState,
                cellular = cellularState,
                sensors = sensorsState,
                blockers = blockers,
            )
        }
    }
}
