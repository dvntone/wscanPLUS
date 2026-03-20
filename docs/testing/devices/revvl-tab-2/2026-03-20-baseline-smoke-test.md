# Revvl Tab 2 Baseline Smoke Test

**Date:** 2026-03-20  
**Device:** `Revvl Tab 2` (`9185W`)  
**Android:** `15`

## Scope

First baseline run using the current debug build after the Phase 2 merges, bug-fix follow-ups, and app-side logging changes.

## Commands used

```powershell
adb -s <device> install -r android\app\build\outputs\apk\debug\app-debug.apk
adb -s <device> shell pm grant com.wscanplus.app android.permission.ACCESS_COARSE_LOCATION
adb -s <device> shell pm grant com.wscanplus.app android.permission.ACCESS_FINE_LOCATION
adb -s <device> shell pm grant com.wscanplus.app android.permission.NEARBY_WIFI_DEVICES
adb -s <device> shell am start -W -n com.wscanplus.app/.MainActivity
adb -s <device> shell dumpsys activity services com.wscanplus.app
adb -s <device> shell cmd wifi list-scan-results
adb -s <device> logcat -d | Select-String -Pattern 'MainActivity|WatchdogService|StandardScanner|ScannerChain'
```

## Observed results

### Build and install

- Fresh debug APK installed successfully.
- Installed app version reported by `dumpsys package`: `0.0.1`.

### Permission gate behavior

- Initial launch still routed through the system permission controller.
- On this Android 15 / OEM build, a usable baseline required explicit ADB grants for:
  - `ACCESS_COARSE_LOCATION`
  - `ACCESS_FINE_LOCATION`
  - `NEARBY_WIFI_DEVICES`
- After those grants, `am start -W` launched `com.wscanplus.app/.MainActivity` directly.

### App/runtime behavior

- `MainActivity` launched successfully after permissions were granted.
- `WatchdogService` was present in `dumpsys activity services`.
- The service was running as a foreground service with `types=0x00000001`.
- The `wscanplus_watchdog` notification channel existed on-device.

### Scan baseline

- `adb shell cmd wifi list-scan-results` returned `No scan results` at capture time.
- This should be treated as a baseline environment observation, not yet as an app defect.

### Logging discrepancy

- Expected app-side tags did not appear in ADB logcat:
  - `MainActivity`
  - `WatchdogService`
  - `StandardScanner`
  - `ScannerChain`
- This is now tracked in issue `#122`.

## Outcome

- The current build is installable and starts on the Revvl Tab 2.
- Foreground-service startup is confirmed.
- The first device-specific blocker for deeper adb verification is missing visible app-side logs on this hardware/OS combination.
