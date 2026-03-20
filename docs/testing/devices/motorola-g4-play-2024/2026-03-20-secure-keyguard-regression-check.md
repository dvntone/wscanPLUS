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

### 2. Pre-fix secure keyguard reproduced the same app-access failure seen on Revvl

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

### 3. Post-fix secure keyguard now restores locked-screen scan retrieval

With the updated build that adds:

- `ACCESS_BACKGROUND_LOCATION`
- `FOREGROUND_SERVICE_LOCATION`
- `WatchdogService` foreground type `location|dataSync`

and with device `location_mode=3` confirmed before the sample:

Observed behavior under real secure keyguard:

- `mBiometricState=STATE_KEYGUARD_AUTH`
- `showing=true`
- `mIsShowing=true`
- `WatchdogService` remained foreground
- shell `cmd wifi list-scan-results` returned populated data
- app logcat showed:

```text
WifiService: getScanResults uid=10407
StandardScanner: Received 24 scan results (24 after stale filter)
WatchdogService: Threats: 8 of 17 signals passed policy gate
```

Observed behavior after unlock / foreground recovery:

- `MainActivity` resumed
- `StandardScanner` received `32` scan results
- `WatchdogService` emitted `Threats: 9 of 19`

## Interpretation

- The pre-fix failure was not limited to Revvl Tab 2 or Android 15.
- The current fix path materially improves the Motorola locked-screen behavior.
- The earlier “no location permission” failure is no longer the dominant result on this device once:
  - background location is declared and granted
  - the service runs as a location-typed foreground service
  - device-wide location mode is actually enabled

## Conclusion

Cross-device evidence supported treating this as a product-level background collection gap, and the current implementation now has positive validation:

- moto g play - 2024 / Android 14 now recovers locked-screen scan access with the current implementation
- Revvl Tab 2 / Android 15 has now been re-tested on current `main` for the non-secure path; keep its secure-lockscreen/background findings open until a post-fix secure-keyguard Revvl validation run happens

The next step is broader validation, not re-proving the old failure:

1. keep checking `location_mode` before drawing scan-access conclusions
2. run the same matrix on the Pixel 10 Pro XL / Android 17 device
3. decide whether "Allow all the time" should be enforced in the operator flow or exposed as a mode requirement

## Related tracking

- `#122` - missing visible app-side adb logs on Revvl Android 15
- `#124` - coarse-only path remains operationally broken on Revvl Android 15
- `#125` - backgrounded / keyguard-visible scan path loses effective location access
