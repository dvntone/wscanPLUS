# Desktop ADB Handoff Data

**Updated**: 2026-03-20

## Purpose

Record the minimum adb checks and runtime data points that the future desktop companion will need
to collect or explain when it starts orchestrating Android installs, permissions, and scan-state
verification.

This note is repo-local on purpose so Android and desktop sessions stay aligned even before the
desktop implementation begins.

## Host-side checks that proved useful

### Install / version state

```powershell
adb shell dumpsys package com.wscanplus.app | Select-String -Pattern 'versionName=|versionCode='
adb install -r path\to\app-debug.apk
adb uninstall com.wscanplus.app
```

Use for:

- confirming which build is on-device
- updating the test app from a trusted host
- cleaning up test installs after a session

### Permission state

```powershell
adb shell dumpsys package com.wscanplus.app | Select-String -Pattern 'ACCESS_FINE_LOCATION|ACCESS_COARSE_LOCATION|ACCESS_BACKGROUND_LOCATION|NEARBY_WIFI_DEVICES'
adb shell cmd appops get com.wscanplus.app
adb shell pm grant com.wscanplus.app android.permission.ACCESS_FINE_LOCATION
adb shell pm grant com.wscanplus.app android.permission.ACCESS_COARSE_LOCATION
adb shell pm grant com.wscanplus.app android.permission.ACCESS_BACKGROUND_LOCATION
adb shell pm grant com.wscanplus.app android.permission.NEARBY_WIFI_DEVICES
adb shell pm revoke com.wscanplus.app android.permission.ACCESS_FINE_LOCATION
adb shell pm revoke com.wscanplus.app android.permission.ACCESS_COARSE_LOCATION
adb shell pm revoke com.wscanplus.app android.permission.ACCESS_BACKGROUND_LOCATION
adb shell pm revoke com.wscanplus.app android.permission.NEARBY_WIFI_DEVICES
```

Use for:

- proving whether the app has the intended runtime state
- separating no-permission, coarse-only, and full-permission behavior

### Location / Wi-Fi environment state

```powershell
adb shell settings get secure location_mode
adb shell settings put secure location_mode 0
adb shell settings put secure location_mode 3
adb shell cmd wifi status
adb shell cmd wifi start-scan
adb shell cmd wifi list-scan-results
```

Interpretation:

- `location_mode=0` means device location is off and will produce platform scan denial even when
  the app has permissions
- shell scan results can still be populated while app-UID scan access fails, so shell data alone
  is not proof that the app itself is working

### Activity / lock / service state

```powershell
adb shell dumpsys activity activities | Select-String -Pattern 'topResumedActivity=|com.wscanplus.app/.MainActivity'
adb shell dumpsys window policy | Select-String -Pattern 'showing=|screenState='
adb shell dumpsys activity services com.wscanplus.app | Select-String -Pattern 'WatchdogService|foreground=true'
adb shell am start -a android.settings.SETTINGS
adb shell am start -n com.wscanplus.app/.MainActivity
adb shell input keyevent 26
```

Use for:

- distinguishing unlocked foreground vs true background vs keyguard samples
- proving whether `WatchdogService` stayed alive during the transition

### App/runtime logs

```powershell
adb logcat -c
adb logcat -d -v brief MainActivity:D WatchdogService:D StandardScanner:D ScannerChain:D WifiService:D *:S
```

High-value signals:

- `Permissions granted: FINE=..., COARSE=...`
- `Using Standard scanner`
- `Scanner started (API ...)`
- `Received ... scan results (... after stale filter)`
- `Threats: ... signals passed policy gate`
- `Permission violation - getScanResults not allowed ...`

## Data the desktop app should eventually surface

- installed app version
- runtime permission summary
- current `location_mode`
- Wi-Fi enabled / scan-always state
- whether the app activity is foregrounded or not
- whether `WatchdogService` is running foreground
- last scan-result count
- last platform denial reason, if any

## Cross-device lessons now established

- Pixel and Motorola default to tethering on connection in this lab setup; Revvl does not
- Revvl is currently the constrained tablet baseline; Pixel and Motorola are the cleaner install /
  tether test targets
- background/keyguard scan continuity must be validated separately from simple foreground success
- coarse-only must be treated as degraded, not equivalent to full scan capability

## OPSEC

- do not place device serials or other persistent device identifiers in tracked docs, issues, PR
  text, or commit messages
- use redacted placeholders in docs and examples
- raw identifiers belong only in local git-ignored artifacts when operationally necessary

