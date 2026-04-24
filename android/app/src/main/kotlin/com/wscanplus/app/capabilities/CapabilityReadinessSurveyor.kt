package com.wscanplus.app.capabilities

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat

class CapabilityReadinessSurveyor(
    private val context: Context,
) : PermissionReadinessProvider {
    override fun current(): PermissionReadiness {
        val blockers = mutableSetOf<ReadinessBlocker>()

        val packageManager = context.packageManager
        val locationManager = context.getSystemService(LocationManager::class.java)
        val wifiManager = context.getSystemService(WifiManager::class.java)
        val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter = bluetoothManager?.adapter

        val hasFineLocation = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        val hasAccessWifiState = hasPermission(Manifest.permission.ACCESS_WIFI_STATE)
        val hasChangeWifiState = hasPermission(Manifest.permission.CHANGE_WIFI_STATE)
        val locationServicesEnabled = locationManager?.isLocationEnabled ?: false
        val hasWifiHardware = packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI)
        val hasNearbyWifiDevices =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                hasPermission(Manifest.permission.NEARBY_WIFI_DEVICES)

        if (!hasFineLocation) blockers += ReadinessBlocker.MISSING_FINE_LOCATION
        if (!hasAccessWifiState) blockers += ReadinessBlocker.MISSING_ACCESS_WIFI_STATE
        if (!hasChangeWifiState) blockers += ReadinessBlocker.MISSING_CHANGE_WIFI_STATE
        if (!locationServicesEnabled) blockers += ReadinessBlocker.LOCATION_SERVICES_DISABLED
        if (!hasNearbyWifiDevices) blockers += ReadinessBlocker.MISSING_NEARBY_WIFI_DEVICES

        val wifiPermissionMissing =
            !hasFineLocation ||
                !hasAccessWifiState ||
                !hasChangeWifiState ||
                !hasNearbyWifiDevices

        val wifiState =
            when {
                !hasWifiHardware || wifiManager == null -> CapabilityState.UNSUPPORTED_BY_HARDWARE
                wifiPermissionMissing -> CapabilityState.SUPPORTED_PERMISSION_MISSING
                !locationServicesEnabled -> CapabilityState.SUPPORTED_DISABLED_BY_SYSTEM
                else -> CapabilityState.SUPPORTED_AND_READY
            }

        val hasBleHardware = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
        val bleScanPermissionMissing =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                !hasPermission(Manifest.permission.BLUETOOTH_SCAN)
            } else {
                !hasFineLocation
            }
        val bleConnectPermissionMissing =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !hasPermission(Manifest.permission.BLUETOOTH_CONNECT)

        if (bleScanPermissionMissing) blockers += ReadinessBlocker.MISSING_BLE_SCAN_PERMISSION
        if (bleConnectPermissionMissing) blockers += ReadinessBlocker.MISSING_BLE_CONNECT_PERMISSION
        if (bluetoothAdapter?.isEnabled == false) blockers += ReadinessBlocker.BLUETOOTH_DISABLED

        val bleState =
            when {
                !hasBleHardware || bluetoothAdapter == null -> CapabilityState.UNSUPPORTED_BY_HARDWARE
                bleScanPermissionMissing || bleConnectPermissionMissing ->
                    CapabilityState.SUPPORTED_PERMISSION_MISSING
                bluetoothAdapter.isEnabled == false -> CapabilityState.SUPPORTED_DISABLED_BY_SYSTEM
                else -> CapabilityState.SUPPORTED_AND_READY
            }

        val cellularState =
            if (hasFineLocation) {
                CapabilityState.SUPPORTED_AND_READY
            } else {
                blockers += ReadinessBlocker.CELL_PERMISSION_MISSING
                CapabilityState.SUPPORTED_PERMISSION_MISSING
            }

        return PermissionReadiness(
            wifi = wifiState,
            ble = bleState,
            cellular = cellularState,
            sensors = CapabilityState.SUPPORTED_AND_READY,
            blockers = blockers,
        )
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
