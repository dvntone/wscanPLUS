# Android Proactive -> Reactive Integration (Verified APIs Only)

Created: 2026-03-19
Status: Implementation-oriented and source-verified
Scope: Android companion app signal intake and escalation logic without deprecated APIs

## Sources (Primary)

- ConnectivityManager.NetworkCallback (API reference)
  https://developer.android.com/reference/android/net/ConnectivityManager.NetworkCallback
- ConnectivityManager.registerDefaultNetworkCallback (API reference)
  https://developer.android.com/reference/android/net/ConnectivityManager
- Network monitoring guide (example patterns)
  https://developer.android.com/training/monitoring-device-state/connectivity-status-type
- WifiManager scan results broadcast (API reference)
  https://developer.android.com/reference/android/net/wifi/WifiManager

## Who / What / When / How / Why

Who
- Android: WatchdogService (or equivalent service) owns signal intake and mode switching
- Desktop: consumes signals over ADB transport per docs/SESSION_STATE.md

What (Supported Signals)
- Network connectivity changes via ConnectivityManager.NetworkCallback
- Scan results availability via WifiManager.SCAN_RESULTS_AVAILABLE_ACTION
- Scan results themselves via WifiManager.getScanResults()

When
- Proactive: always-on lightweight monitoring (NetworkCallback + passive scan results)
- Reactive: triggered when the default network is lost or when local heuristics flag an anomaly

How (Verified API Surface)
- Register NetworkCallback via registerDefaultNetworkCallback
- Listen for SCAN_RESULTS_AVAILABLE_ACTION and read WifiManager.getScanResults()
- Run existing heuristics only on new or changed scan batches

Why
- NetworkCallback is the supported replacement for deprecated NetworkInfo state APIs
- SCAN_RESULTS_AVAILABLE_ACTION is the supported signal for scan updates
- Passive scan ingestion reduces battery impact while preserving detection

## Verified Implementation Pattern (Kotlin)

Register NetworkCallback (requires ACCESS_NETWORK_STATE)

```kotlin
val cm = getSystemService(ConnectivityManager::class.java)
val callback = object : ConnectivityManager.NetworkCallback() {
    override fun onAvailable(network: Network) {
        // Network became available
    }

    override fun onLost(network: Network) {
        // Default network lost
        // Use this as a trigger to enter reactive mode
    }
}
cm.registerDefaultNetworkCallback(callback)
```

Reference: ConnectivityManager.NetworkCallback, registerDefaultNetworkCallback
https://developer.android.com/reference/android/net/ConnectivityManager.NetworkCallback
https://developer.android.com/reference/android/net/ConnectivityManager

Listen for scan results

```kotlin
val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
registerReceiver(receiver, filter)

// in receiver
val results = wifiManager.scanResults
```

Reference: WifiManager.SCAN_RESULTS_AVAILABLE_ACTION and getScanResults()
https://developer.android.com/reference/android/net/wifi/WifiManager

## Proactive -> Reactive Escalation (Non-Guessing Policy)

Proactive (default)
- Use NetworkCallback for connectivity changes
- Consume scan results when available (no active scan loops)
- Run existing local heuristics only on changed result sets

Reactive (triggered)
- Trigger when NetworkCallback.onLost() fires for the default network
- Trigger when local heuristics produce signals over the configured threshold

Notes
- This uses only documented, supported API signals
- The trigger thresholds are part of heuristic policy (already defined in Phase 2 docs)

## Explicitly Not Used (Deprecated / Unsupported)

Do not rely on these for core logic:
- WifiManager.SUPPLICANT_STATE_CHANGED_ACTION (deprecated API 28)
- WifiManager.EXTRA_SUPPLICANT_ERROR (deprecated API 28)
- WifiManager.ERROR_AUTHENTICATING (deprecated API 28)
- NetworkInfo and NetworkInfo.DetailedState (deprecated API 29)

References for deprecations:
https://developer.android.com/reference/android/net/wifi/WifiManager
https://developer.android.com/reference/android/net/NetworkInfo
