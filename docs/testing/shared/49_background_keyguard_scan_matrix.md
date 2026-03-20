# Background / Keyguard Scan Matrix

**Updated**: 2026-03-20 (America/Los_Angeles)

## Purpose

Standardize the device test needed for wscan+ long-running collection behavior on modern Android.

This matrix exists because foreground-service survival alone has proven insufficient to preserve
`WifiManager.getScanResults()` access once the app is no longer actively foregrounded.

## Preconditions

- latest debug build installed
- `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`, and `NEARBY_WIFI_DEVICES` granted
- device location services enabled
- Wi-Fi enabled
- `WatchdogService` confirmed foreground
- confirm `adb shell settings get secure location_mode` is not `0` before concluding the app lacks scan access

## Matrix

### 1. Unlocked foreground control

Expected goal:

- `MainActivity` resumed
- app UID can access `getScanResults()`
- `StandardScanner` receives results

Suggested checks:

```powershell
adb shell dumpsys activity top | Select-String -Pattern 'com.wscanplus.app|mResumed=true'
adb shell cmd wifi start-scan
adb logcat -d | Select-String -Pattern 'getScanResults uid=|Permission violation|StandardScanner|Threats:'
```

### 2. HOME / background transition

Expected goal:

- determine whether scan retrieval survives when UI is no longer visible

Suggested checks:

```powershell
adb shell input keyevent KEYCODE_HOME
adb shell dumpsys activity services com.wscanplus.app
adb shell cmd wifi start-scan
adb logcat -d | Select-String -Pattern 'getScanResults uid=|Permission violation|StandardScanner|Threats:'
```

### 3. Secure keyguard transition

Expected goal:

- determine whether scan retrieval survives when the device is locked or waiting for biometric/PIN auth

Suggested checks:

```powershell
adb shell dumpsys window policy | Select-String -Pattern 'showing=true|mIsShowing|mBiometricState|screenState'
adb shell dumpsys activity services com.wscanplus.app
adb shell cmd wifi start-scan
adb shell cmd wifi list-scan-results
adb logcat -d | Select-String -Pattern 'getScanResults uid=|Permission violation|StandardScanner|Threats:'
```

### 4. Post-unlock recovery

Expected goal:

- determine whether scan access returns immediately after the user unlocks

Suggested checks:

```powershell
adb shell am start -W -n com.wscanplus.app/.MainActivity
adb shell dumpsys activity top | Select-String -Pattern 'com.wscanplus.app|mResumed=true'
adb shell cmd wifi start-scan
adb logcat -d | Select-String -Pattern 'getScanResults uid=|Permission violation|StandardScanner|Threats:'
```

## Interpretation rules

- If shell `cmd wifi list-scan-results` returns populated data while the app UID gets
  `Permission violation - getScanResults not allowed ... has no location permission`,
  treat that as app-access-scope failure rather than radio failure.
- If the app succeeds while unlocked and fails only after HOME or secure keyguard, treat that
  as evidence that while-in-use location scope is insufficient for the intended collection mode.
- If failure reason is `Location mode is disabled for the device`, correct that before drawing
  background/keyguard conclusions.

## Current cross-device result

As of 2026-03-20:

- pre-fix baseline:
  - Revvl Tab 2 (Android 15): unlocked foreground worked; HOME and keyguard broke app scan access
  - moto g play - 2024 (Android 14): unlocked foreground worked; secure keyguard broke app scan access
- post-fix validation with `ACCESS_BACKGROUND_LOCATION` + `foregroundServiceType="location|dataSync"`:
  - moto g play - 2024 (Android 14): secure keyguard now allows `getScanResults()` and `StandardScanner` receives fresh results again
  - Revvl Tab 2 (Android 15): not yet re-tested after the permission/service fixes landed on current main; do not treat the older Revvl outcome as post-fix validation
  - Pixel 10 Pro XL beta-track device: unlocked foreground, true background, secure keyguard, and post-unlock recovery all pass with fresh scan results and threat output

Related limitation that remains outside the background/keyguard fix:

- coarse-only permission on the Pixel still launches the app and service, but did not produce usable scan-result callbacks during the full matrix run

This remains a cross-device issue area, but the current fix path now has positive evidence on both test devices.
