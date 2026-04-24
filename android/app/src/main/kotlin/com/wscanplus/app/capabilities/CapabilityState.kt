package com.wscanplus.app.capabilities

import kotlinx.serialization.Serializable

/**
 * Runtime capability state for a scan module or supporting detector.
 *
 * This intentionally distinguishes hardware support from current usability.
 * A device may support Wi-Fi/BLE/etc. while the app is still blocked by
 * permissions, disabled system services, battery policy, stale results, or
 * an untested/OEM-specific path.
 */
@Serializable
enum class CapabilityState {
    SUPPORTED_AND_READY,
    SUPPORTED_PERMISSION_MISSING,
    SUPPORTED_DISABLED_BY_SYSTEM,
    SUPPORTED_BLOCKED_BY_BATTERY,
    SUPPORTED_BUT_STALE,
    UNSUPPORTED_BY_HARDWARE,
    UNSUPPORTED_BY_OS,
    UNKNOWN_UNTESTED,
}

@Serializable
enum class ReadinessBlocker {
    MISSING_WIFI_PERMISSION,
    MISSING_CHANGE_WIFI_STATE,
    MISSING_ACCESS_WIFI_STATE,
    MISSING_FINE_LOCATION,
    MISSING_NEARBY_WIFI_DEVICES,
    LOCATION_SERVICES_DISABLED,

    MISSING_BLE_SCAN_PERMISSION,
    MISSING_BLE_CONNECT_PERMISSION,
    BLUETOOTH_DISABLED,

    CELL_PERMISSION_MISSING,

    BACKGROUND_LOCATION_MISSING,
    BACKGROUND_RESTRICTED,
    BATTERY_OPTIMIZATION_ACTIVE,
    FOREGROUND_SERVICE_NOT_RUNNING,

    SCAN_THROTTLED,
    SCAN_RESULTS_STALE,
    DEVICE_OEM_QUIRK_ACTIVE,

    SENSOR_UNAVAILABLE,
    UNKNOWN,
}

@Serializable
data class PermissionReadiness(
    val wifi: CapabilityState,
    val ble: CapabilityState,
    val cellular: CapabilityState,
    val sensors: CapabilityState,
    val blockers: Set<ReadinessBlocker>,
    val checkedAtMs: Long = System.currentTimeMillis(),
) {
    val canRunMinimumScan: Boolean
        get() =
            wifi == CapabilityState.SUPPORTED_AND_READY &&
                !blockers.contains(ReadinessBlocker.LOCATION_SERVICES_DISABLED)

    val degraded: Boolean
        get() =
            blockers.isNotEmpty() ||
                wifi != CapabilityState.SUPPORTED_AND_READY ||
                ble != CapabilityState.SUPPORTED_AND_READY ||
                cellular != CapabilityState.SUPPORTED_AND_READY ||
                sensors != CapabilityState.SUPPORTED_AND_READY
}

interface PermissionReadinessProvider {
    fun current(): PermissionReadiness
}
