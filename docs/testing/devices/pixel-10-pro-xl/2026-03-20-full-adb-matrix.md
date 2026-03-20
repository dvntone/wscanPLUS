# Pixel 10 Pro XL Full ADB Matrix

**Date**: 2026-03-20  
**Build under test**: `com.wscanplus.app` `versionName=0.1.0`, `versionCode=2`  
**Platform values reported by adb**:

- `ro.build.version.release=16`
- `ro.build.version.sdk=36`
- `ro.build.version.codename=CinnamonBun`

## Scope

Run the full permission and runtime matrix on the current Pixel beta device with:

- Advanced Protection enabled
- developer options enabled
- Wi-Fi scan throttling disabled
- current background/keyguard fix on `main`

This pass intentionally re-ran the matrix sequentially after an earlier parallel permission test
produced contaminated results.

## Preconditions used for the clean pass

- Wi-Fi enabled
- trusted-host adb session already authorized
- device location mode explicitly checked before each conclusion
- app logs filtered by:
  - `MainActivity`
  - `WatchdogService`
  - `StandardScanner`
  - `ScannerChain`
  - `WifiService`

## Results

### 1. No-permission launch

Observed behavior:

- the app landed in the Android location permission controller
- the first prompt offered:
  - precise vs approximate
  - while-using vs one-time vs deny

Conclusion:

- current operator flow correctly surfaces the system permission gate instead of failing silently

### 2. Coarse-only launch

Granted state:

- `ACCESS_COARSE_LOCATION`
- `NEARBY_WIFI_DEVICES`
- `ACCESS_FINE_LOCATION` revoked
- `ACCESS_BACKGROUND_LOCATION` revoked

Observed behavior:

- `MainActivity` logged `Permissions granted: FINE=false, COARSE=true`
- `WatchdogService` started
- `StandardScanner` started
- the UI remained on `Starting scanner...`
- shell `cmd wifi list-scan-results` remained populated
- the app did not produce fresh `Received ... scan results` callbacks during the coarse-only sample

Conclusion:

- coarse-only is still a degraded state on the current Pixel path
- it no longer dead-ends startup, but it is not a reliable scan-capable mode

### 3. Full-permission foreground

Granted state:

- `ACCESS_FINE_LOCATION`
- `ACCESS_COARSE_LOCATION`
- `ACCESS_BACKGROUND_LOCATION`
- `NEARBY_WIFI_DEVICES`

Observed behavior:

- `MainActivity` resumed
- `WifiService` logged `getScanResults uid=...`
- `StandardScanner` received `49` fresh results on the first clean foreground control
- `StandardScanner` later received `52` fresh results on the final unlocked foreground control
- `WatchdogService` produced threat output in both foreground samples

Conclusion:

- full foreground scan path is working on the current Pixel build

### 4. True background sample

Method:

- launch Android Settings to move `MainActivity` out of top-resumed state

Observed behavior:

- `topResumedActivity=com.android.settings/.Settings`
- `WatchdogService` remained foreground
- `WifiService` logged `getScanResults uid=...`
- `StandardScanner` received `47` fresh results
- `WatchdogService` produced threat output

Conclusion:

- the current background-location + location-typed foreground-service fix is holding in true background state on this Pixel

### 5. Secure keyguard continuity

Observed behavior:

- `showing=true`
- `screenState=SCREEN_STATE_ON`
- top surface was a non-app keyguard/dream activity
- `WatchdogService` remained foreground
- `WifiService` logged `getScanResults uid=...`
- `StandardScanner` received `52` fresh results
- `WatchdogService` produced threat output

Conclusion:

- locked-screen scan continuity is working on the current Pixel setup

### 6. Post-unlock recovery

Observed behavior:

- unlocked-background recovery succeeded while Settings stayed top-resumed
- unlocked-foreground recovery succeeded after bringing `MainActivity` back to the front
- final unlocked foreground control produced `52` fresh results and threat output

Conclusion:

- post-unlock recovery is normal on this device after the current fix

### 7. Location-off failure control

Method:

- set `location_mode=0`

Observed behavior:

- `WifiService` logged:
  - `Permission violation - getScanResults not allowed ... reason=Location mode is disabled for the device`
- `StandardScanner` received `0` scan results
- `WatchdogService` produced `0 of 0` signals
- location mode was restored immediately after the control

Conclusion:

- the app still reflects the expected platform denial when device location is off
- the background/keyguard fix did not mask that underlying requirement

## Advanced Protection result

Observed earlier on the same device/session:

- trusted-host `adb install -r` succeeded while Advanced Protection was enabled
- a repeat `adb install -r` also succeeded with the lockscreen showing

Interpretation:

- in the current trusted-host setup, Advanced Protection is not blocking test-app install/update
- future desktop onboarding should still distinguish:
  - first-trust vs already-trusted host
  - install/update vs shell access
  - unlocked vs locked device state

## Desktop handoff relevance

This run confirms the future desktop companion needs host-side checks for:

- app version currently installed
- runtime permission state
- `location_mode`
- whether the app is foregrounded, backgrounded, or behind keyguard
- whether `WatchdogService` remains foreground
- whether `WifiService` is returning app-UID scan access or a permission-violation reason

See `docs/testing/shared/50_desktop_adb_handoff.md`.

## Post-session device state

At the end of this session:

- device location was set back to off (`location_mode=0`)
- the test app was removed from the device

