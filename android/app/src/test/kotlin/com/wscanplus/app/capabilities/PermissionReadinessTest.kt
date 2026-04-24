package com.wscanplus.app.capabilities

import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionReadinessTest {
    @Test
    fun canRunMinimumScan_requires_ready_wifi() {
        val readiness =
            PermissionReadiness(
                wifi = CapabilityState.SUPPORTED_PERMISSION_MISSING,
                ble = CapabilityState.SUPPORTED_AND_READY,
                cellular = CapabilityState.SUPPORTED_AND_READY,
                sensors = CapabilityState.SUPPORTED_AND_READY,
                blockers = emptySet(),
            )

        assertFalse(readiness.canRunMinimumScan)
        assertTrue(readiness.degraded)
    }

    @Test
    fun canRunMinimumScan_blocks_when_location_services_disabled() {
        val readiness =
            PermissionReadiness(
                wifi = CapabilityState.SUPPORTED_AND_READY,
                ble = CapabilityState.SUPPORTED_AND_READY,
                cellular = CapabilityState.SUPPORTED_AND_READY,
                sensors = CapabilityState.SUPPORTED_AND_READY,
                blockers = setOf(ReadinessBlocker.LOCATION_SERVICES_DISABLED),
            )

        assertFalse(readiness.canRunMinimumScan)
    }

    @Test
    fun canRunMinimumScan_allows_ready_wifi_without_location_services_blocker() {
        val readiness =
            PermissionReadiness(
                wifi = CapabilityState.SUPPORTED_AND_READY,
                ble = CapabilityState.SUPPORTED_AND_READY,
                cellular = CapabilityState.SUPPORTED_AND_READY,
                sensors = CapabilityState.SUPPORTED_AND_READY,
                blockers = emptySet(),
            )

        assertTrue(readiness.canRunMinimumScan)
    }

    @Test
    fun degraded_false_when_every_module_ready_and_no_blockers() {
        val readiness =
            PermissionReadiness(
                wifi = CapabilityState.SUPPORTED_AND_READY,
                ble = CapabilityState.SUPPORTED_AND_READY,
                cellular = CapabilityState.SUPPORTED_AND_READY,
                sensors = CapabilityState.SUPPORTED_AND_READY,
                blockers = emptySet(),
            )

        assertFalse(readiness.degraded)
    }

    @Test
    fun degraded_true_when_any_blocker_exists() {
        val readiness =
            PermissionReadiness(
                wifi = CapabilityState.SUPPORTED_AND_READY,
                ble = CapabilityState.SUPPORTED_AND_READY,
                cellular = CapabilityState.SUPPORTED_AND_READY,
                sensors = CapabilityState.SUPPORTED_AND_READY,
                blockers = setOf(ReadinessBlocker.SCAN_RESULTS_STALE),
            )

        assertTrue(readiness.degraded)
    }

    @Test
    fun degraded_true_when_ble_not_ready() {
        val readiness =
            PermissionReadiness(
                wifi = CapabilityState.SUPPORTED_AND_READY,
                ble = CapabilityState.SUPPORTED_PERMISSION_MISSING,
                cellular = CapabilityState.SUPPORTED_AND_READY,
                sensors = CapabilityState.SUPPORTED_AND_READY,
                blockers = emptySet(),
            )

        assertTrue(readiness.degraded)
    }

    @Test
    fun readiness_blockers_include_required_issue_categories() {
        val required =
            setOf(
                ReadinessBlocker.MISSING_FINE_LOCATION,
                ReadinessBlocker.LOCATION_SERVICES_DISABLED,
                ReadinessBlocker.MISSING_BLE_SCAN_PERMISSION,
                ReadinessBlocker.MISSING_BLE_CONNECT_PERMISSION,
                ReadinessBlocker.BATTERY_OPTIMIZATION_ACTIVE,
                ReadinessBlocker.FOREGROUND_SERVICE_NOT_RUNNING,
                ReadinessBlocker.SCAN_RESULTS_STALE,
            )

        assertTrue(ReadinessBlocker.entries.containsAll(required))
    }

    @Test
    fun evaluate_ready_baseline_reports_all_modules_ready() {
        val readiness = CapabilityReadinessSurveyor.evaluate(readyInputs())

        assertEquals(CapabilityState.SUPPORTED_AND_READY, readiness.wifi)
        assertEquals(CapabilityState.SUPPORTED_AND_READY, readiness.ble)
        assertEquals(CapabilityState.SUPPORTED_AND_READY, readiness.cellular)
        assertEquals(CapabilityState.SUPPORTED_AND_READY, readiness.sensors)
        assertTrue(readiness.canRunMinimumScan)
        assertFalse(readiness.degraded)
    }

    @Test
    fun evaluate_location_services_disabled_marks_wifi_disabled_and_blocks_minimum_scan() {
        val readiness =
            CapabilityReadinessSurveyor.evaluate(
                readyInputs(locationServicesEnabled = false),
            )

        assertEquals(CapabilityState.SUPPORTED_DISABLED_BY_SYSTEM, readiness.wifi)
        assertTrue(readiness.blockers.contains(ReadinessBlocker.LOCATION_SERVICES_DISABLED))
        assertFalse(readiness.canRunMinimumScan)
        assertTrue(readiness.degraded)
    }

    @Test
    fun evaluate_does_not_emit_wifi_specific_blockers_without_wifi_support() {
        val readiness =
            CapabilityReadinessSurveyor.evaluate(
                readyInputs(
                    hasWifiHardware = false,
                    hasAccessWifiState = false,
                    hasChangeWifiState = false,
                    hasNearbyWifiDevicesPermission = false,
                ),
            )

        assertEquals(CapabilityState.UNSUPPORTED_BY_HARDWARE, readiness.wifi)
        assertFalse(readiness.blockers.contains(ReadinessBlocker.MISSING_ACCESS_WIFI_STATE))
        assertFalse(readiness.blockers.contains(ReadinessBlocker.MISSING_CHANGE_WIFI_STATE))
        assertFalse(readiness.blockers.contains(ReadinessBlocker.MISSING_NEARBY_WIFI_DEVICES))
    }

    @Test
    fun evaluate_marks_wifi_unsupported_when_wifi_manager_unavailable() {
        val readiness = CapabilityReadinessSurveyor.evaluate(readyInputs(hasWifiManager = false))

        assertEquals(CapabilityState.UNSUPPORTED_BY_HARDWARE, readiness.wifi)
        assertTrue(readiness.degraded)
    }

    @Test
    fun evaluate_marks_cellular_unsupported_without_telephony_support() {
        val readiness = CapabilityReadinessSurveyor.evaluate(readyInputs(hasTelephonyHardware = false))

        assertEquals(CapabilityState.UNSUPPORTED_BY_HARDWARE, readiness.cellular)
    }

    @Test
    fun evaluate_marks_sensors_unsupported_when_no_supported_sensor_exists() {
        val readiness = CapabilityReadinessSurveyor.evaluate(readyInputs(hasAnySupportedSensor = false))

        assertEquals(CapabilityState.UNSUPPORTED_BY_HARDWARE, readiness.sensors)
        assertTrue(readiness.blockers.contains(ReadinessBlocker.SENSOR_UNAVAILABLE))
    }

    @Test
    fun evaluate_marks_ble_scan_and_connect_permissions_separately_on_android_s_plus() {
        val readiness =
            CapabilityReadinessSurveyor.evaluate(
                readyInputs(
                    sdkInt = Build.VERSION_CODES.S,
                    hasBluetoothScanPermission = false,
                    hasBluetoothConnectPermission = false,
                ),
            )

        assertEquals(CapabilityState.SUPPORTED_PERMISSION_MISSING, readiness.ble)
        assertTrue(readiness.blockers.contains(ReadinessBlocker.MISSING_BLE_SCAN_PERMISSION))
        assertTrue(readiness.blockers.contains(ReadinessBlocker.MISSING_BLE_CONNECT_PERMISSION))
    }

    @Test
    fun evaluate_does_not_emit_ble_blockers_without_ble_support() {
        val readiness =
            CapabilityReadinessSurveyor.evaluate(
                readyInputs(
                    sdkInt = Build.VERSION_CODES.S,
                    hasBleHardware = false,
                    hasBluetoothAdapter = false,
                    bluetoothEnabled = false,
                    hasBluetoothScanPermission = false,
                    hasBluetoothConnectPermission = false,
                ),
            )

        assertEquals(CapabilityState.UNSUPPORTED_BY_HARDWARE, readiness.ble)
        assertFalse(readiness.blockers.contains(ReadinessBlocker.MISSING_BLE_SCAN_PERMISSION))
        assertFalse(readiness.blockers.contains(ReadinessBlocker.MISSING_BLE_CONNECT_PERMISSION))
        assertFalse(readiness.blockers.contains(ReadinessBlocker.BLUETOOTH_DISABLED))
    }

    @Test
    fun evaluate_pre_s_ble_location_services_disabled_marks_ble_disabled() {
        val readiness =
            CapabilityReadinessSurveyor.evaluate(
                readyInputs(
                    sdkInt = Build.VERSION_CODES.R,
                    locationServicesEnabled = false,
                ),
            )

        assertEquals(CapabilityState.SUPPORTED_DISABLED_BY_SYSTEM, readiness.ble)
        assertTrue(readiness.blockers.contains(ReadinessBlocker.LOCATION_SERVICES_DISABLED))
    }

    @Test
    fun evaluate_pre_s_missing_fine_location_does_not_emit_modern_ble_scan_permission_blocker() {
        val readiness =
            CapabilityReadinessSurveyor.evaluate(
                readyInputs(
                    sdkInt = Build.VERSION_CODES.R,
                    hasFineLocation = false,
                ),
            )

        assertEquals(CapabilityState.SUPPORTED_PERMISSION_MISSING, readiness.ble)
        assertTrue(readiness.blockers.contains(ReadinessBlocker.MISSING_FINE_LOCATION))
        assertFalse(readiness.blockers.contains(ReadinessBlocker.MISSING_BLE_SCAN_PERMISSION))
    }

    private fun readyInputs(
        sdkInt: Int = Build.VERSION_CODES.TIRAMISU,
        hasFineLocation: Boolean = true,
        hasAccessWifiState: Boolean = true,
        hasChangeWifiState: Boolean = true,
        hasNearbyWifiDevicesPermission: Boolean = true,
        locationServicesEnabled: Boolean = true,
        hasWifiHardware: Boolean = true,
        hasWifiManager: Boolean = true,
        hasBleHardware: Boolean = true,
        hasBluetoothAdapter: Boolean = true,
        bluetoothEnabled: Boolean = true,
        hasBluetoothScanPermission: Boolean = true,
        hasBluetoothConnectPermission: Boolean = true,
        hasTelephonyHardware: Boolean = true,
        hasTelephonyManager: Boolean = true,
        hasAnySupportedSensor: Boolean = true,
    ): CapabilityReadinessSurveyor.ReadinessInputs =
        CapabilityReadinessSurveyor.ReadinessInputs(
            sdkInt = sdkInt,
            hasFineLocation = hasFineLocation,
            hasAccessWifiState = hasAccessWifiState,
            hasChangeWifiState = hasChangeWifiState,
            hasNearbyWifiDevicesPermission = hasNearbyWifiDevicesPermission,
            locationServicesEnabled = locationServicesEnabled,
            hasWifiHardware = hasWifiHardware,
            hasWifiManager = hasWifiManager,
            hasBleHardware = hasBleHardware,
            hasBluetoothAdapter = hasBluetoothAdapter,
            bluetoothEnabled = bluetoothEnabled,
            hasBluetoothScanPermission = hasBluetoothScanPermission,
            hasBluetoothConnectPermission = hasBluetoothConnectPermission,
            hasTelephonyHardware = hasTelephonyHardware,
            hasTelephonyManager = hasTelephonyManager,
            hasAnySupportedSensor = hasAnySupportedSensor,
        )
}
