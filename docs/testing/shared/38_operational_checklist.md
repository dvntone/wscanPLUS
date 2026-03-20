# Operational Test Checklist (Reproducible)

**Updated**: 2026-03-20 (America/Los_Angeles)

## Purpose

Provide a repeatable checklist that matches the current app state:

- Phase 2 local threat intelligence is merged
- App-side logging exists for `MainActivity`, `WatchdogService`, `StandardScanner`, and `ScannerChain`
- Android 15 service behavior matters because `WatchdogService` runs as `dataSync`

## Phase-aligned smoke test

1. Install the latest debug build:
```powershell
adb install -r android\app\build\outputs\apk\debug\app-debug.apk
```
2. Clear prior logs and launch the app:
```powershell
adb logcat -c
adb shell monkey -p com.wscanplus.app -c android.intent.category.LAUNCHER 1
```
3. Verify the app emits the expected tags:
```powershell
adb logcat -d | Select-String -Pattern 'MainActivity|WatchdogService|StandardScanner|ScannerChain'
```
4. Verify package/runtime state:
```powershell
adb shell dumpsys package com.wscanplus.app | Select-String -Pattern 'requested permissions|ACCESS_FINE_LOCATION|ACCESS_COARSE_LOCATION' -Context 0,20
adb shell cmd appops get com.wscanplus.app
adb shell dumpsys activity services com.wscanplus.app | Select-String -Pattern 'WatchdogService|foreground' -Context 0,2
```

## Permission behavior checks

### Full location path

Grant location and confirm service/scanner startup logs appear:

```powershell
adb shell pm grant com.wscanplus.app android.permission.ACCESS_COARSE_LOCATION
adb shell pm grant com.wscanplus.app android.permission.ACCESS_FINE_LOCATION
adb logcat -c
adb shell monkey -p com.wscanplus.app -c android.intent.category.LAUNCHER 1
adb logcat -d | Select-String -Pattern 'Permissions granted|Service started|Scanner started|Using Standard scanner|Using USB scanner'
```

### Coarse-only path

On Android 12+, verify the app does not dead-end when only coarse location is available:

```powershell
adb shell pm revoke com.wscanplus.app android.permission.ACCESS_FINE_LOCATION
adb shell pm grant com.wscanplus.app android.permission.ACCESS_COARSE_LOCATION
adb logcat -c
adb shell monkey -p com.wscanplus.app -c android.intent.category.LAUNCHER 1
adb logcat -d | Select-String -Pattern 'Permissions granted|Service started|Permission denied'
```

Expected result:

- `MainActivity` should report `FINE=false, COARSE=true`
- The app should proceed in degraded mode rather than block immediately

## Android 15 service permission guard

Revoke location and confirm the sticky-restart guard is doing the right thing:

```powershell
adb shell pm revoke com.wscanplus.app android.permission.ACCESS_FINE_LOCATION
adb shell pm revoke com.wscanplus.app android.permission.ACCESS_COARSE_LOCATION
adb logcat -c
adb shell am startservice -n com.wscanplus.app/.WatchdogService
adb logcat -d | Select-String -Pattern 'Permission denied, stopping|WatchdogService'
```

Expected result:

- `WatchdogService` logs the permission denial path
- The scanner does not start without location permission

## WiFi scan behavior checks

### WiFi OFF with scan-always ON

```powershell
adb shell settings put global wifi_scan_always_enabled 1
adb shell cmd wifi set-wifi-enabled disabled
adb shell cmd wifi list-scan-results
```

### WiFi OFF with scan-always OFF

```powershell
adb shell settings put global wifi_scan_always_enabled 0
adb shell cmd wifi set-wifi-enabled disabled
adb shell cmd wifi list-scan-results
```

## Supporting environment checks

### Altitude (last known)

```powershell
adb shell dumpsys location | Select-String -Pattern 'last location=Location\[fused|last location=Location\[network'
```

### Mock altitude (FakeGPS Route)

1. Set mock app:
```powershell
adb shell settings put secure mock_location_app com.incorporateapps.fakegps_route
```
2. In app, set a location and altitude.
3. Verify:
```powershell
adb shell dumpsys location | Select-String -Pattern 'mock' -Context 0,1
```

### Bluetooth state

```powershell
adb shell settings get global bluetooth_on
adb shell dumpsys bluetooth_manager | Select-String -Pattern 'enabled|state|BLE|LE' -Context 0,1
```
