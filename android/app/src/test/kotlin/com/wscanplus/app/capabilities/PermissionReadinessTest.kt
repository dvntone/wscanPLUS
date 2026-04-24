package com.wscanplus.app.capabilities

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
}
