# Motorola G4 Play 2024 Secure Keyguard Regression Check

**Date:** 2026-03-20  
**Device:** `moto g play - 2024`  
**Android:** `14`

## Purpose

Re-check the previous Motorola baseline against the current Phase 2 build using a real secure
lock state, and compare the result with the Revvl Tab 2 Android 15 session.

## Findings

### 1. Foreground path remains healthy

With:

- `ACCESS_COARSE_LOCATION=true`
- `ACCESS_FINE_LOCATION=true`
- `NEARBY_WIFI_DEVICES=true`
- location services on

Observed behavior:

- `MainActivity` resumed
- `WatchdogService` stayed foreground
- app-side logs were visible in adb
- `StandardScanner` started successfully

### 2. Secure keyguard reproduces the same app-access failure seen on Revvl

With:

- secure keyguard active
- `mBiometricState=STATE_KEYGUARD_AUTH`
- `showing=true`
- `mIsShowing=true`
- `WatchdogService` still foreground

Observed behavior:

- shell `cmd wifi list-scan-results` still returned populated scan data
- app logcat showed:

```text
WifiService: Permission violation - getScanResults not allowed ... UID 10407 has no location permission
```

- `StandardScanner` received `0` results
- `WatchdogService` emitted `Threats: 0 of 0`

## Interpretation

- The secure-lock failure is not limited to Revvl Tab 2 or Android 15.
- The current app model loses effective scan-result access once the app is no longer actively foregrounded, even though:
  - the foreground service survives
  - shell-level Wi-Fi scans still work

## Conclusion

Cross-device evidence now supports treating this as a product-level background collection gap:

- Revvl Tab 2 / Android 15 reproduces it
- moto g play - 2024 / Android 14 reproduces it under a real secure keyguard state

The next step is implementation, not more passive reproduction:

1. add `ACCESS_BACKGROUND_LOCATION`
2. revisit `WatchdogService` foreground-service typing for location-sensitive work
3. rebuild
4. rerun the unlocked -> HOME -> secure keyguard matrix

## Related tracking

- `#122` - missing visible app-side adb logs on Revvl Android 15
- `#124` - coarse-only path remains operationally broken on Revvl Android 15
- `#125` - backgrounded / keyguard-visible scan path loses effective location access
