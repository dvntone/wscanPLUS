# Revvl Tab 2 Full ADB Matrix

**Date:** 2026-03-20  
**Device:** `Revvl Tab 2`  
**Android:** `15`

## Purpose

Expand the first baseline into a broader device-validation pass covering:

- no-permission behavior
- coarse-only behavior
- full-fine behavior
- Wi-Fi ON/OFF scan shell behavior
- location and Bluetooth environment state
- comparison point via WiFiAnalyzer

## Starting device state

- `location_mode=0` at the start of the session
- `wifi_scan_always_enabled=0` at the start of the session
- Bluetooth off
- `WiFiAnalyzer` installed (`3.2.2`)

## Findings

### 1. No-permission launch behaves correctly

- Launching with location and nearby Wi-Fi permissions revoked routes to the system permission controller.
- `WatchdogService` is not left running in this state.

Interpretation:

- The app is correctly blocked before scanner startup when runtime permissions are missing.

### 2. Shell scan behavior depends strongly on environment state

With `location_mode=3`, `wifi_scan_always_enabled=1`, and Wi-Fi enabled:

- `adb shell cmd wifi list-scan-results` returned a populated network list.

With Wi-Fi disabled:

- `wifi_scan_always_enabled=1` -> `No scan results`
- `wifi_scan_always_enabled=0` -> `No scan results`

Interpretation:

- This Revvl Tab 2 does not mirror the earlier Motorola behavior where Wi-Fi-off plus scan-always-on still yielded shell scan results.
- That is a hardware/OEM baseline difference, not an app-only signal.

### 3. Coarse-only launch no longer hard-blocks, but scanning is still impaired

With:

- `ACCESS_COARSE_LOCATION=true`
- `ACCESS_FINE_LOCATION=false`
- `NEARBY_WIFI_DEVICES=true`
- location services on
- Wi-Fi on

Observed behavior:

- `MainActivity` launches
- `WatchdogService` starts and remains foreground
- the app UI stays on `Starting scanner...`
- logcat includes:

```text
WifiService: Permission violation - getScanResults not allowed ... has no location permission
```

Interpretation:

- The coarse-only fallback is partially effective: the app no longer dead-ends at launch.
- It is not fully effective on this device because actual scan retrieval still fails without effective fine location.

### 4. Full-fine path enables scanner registration

With:

- `ACCESS_COARSE_LOCATION=true`
- `ACCESS_FINE_LOCATION=true`
- `NEARBY_WIFI_DEVICES=true`
- location services on
- Wi-Fi on

Observed behavior:

- `MainActivity` launches
- `WatchdogService` remains active as a foreground service
- logcat shows `WifiService` activity for:
  - `getVerboseLoggingLevel`
  - `registerScanResultsCallback`
- the earlier `getScanResults not allowed ... has no location permission` error is absent from this path

Interpretation:

- Full-fine permission is operationally required for the current scan path on this tablet.
- This is the strongest evidence from the session because it isolates the difference from the coarse-only path.

### 4a. Backgrounding and keyguard break scan retrieval even when full-fine is granted

With:

- `ACCESS_COARSE_LOCATION=true`
- `ACCESS_FINE_LOCATION=true`
- `NEARBY_WIFI_DEVICES=true`
- location services on
- Wi-Fi on

Observed behavior while `MainActivity` is visible:

- logcat shows `WifiService: getScanResults` for the app UID
- the `getScanResults not allowed ... has no location permission` failure is absent

Observed behavior after sending the app to HOME:

- `WatchdogService` remains active as a foreground service
- shell-level `cmd wifi list-scan-results` still returns populated scan data
- app logcat shifts to:

```text
WifiService: Permission violation - getScanResults not allowed ... UID 10261 has no location permission
```

Observed behavior on keyguard with screen on and lockscreen showing:

- `KeyguardServiceDelegate showing=true`
- `screenState=SCREEN_STATE_ON`
- `WatchdogService` still remains foreground
- the same `getScanResults not allowed ... has no location permission` failure appears for the app UID

Interpretation:

- On this Android 15 device, foreground-service status alone is not enough to preserve scan-result access once the UI is no longer actively foregrounded.
- For wscan+'s discreet or long-running field-collection role, background location handling must be treated as an active product/implementation question rather than a policy afterthought.

### 5. App-side logs are still not visible in adb logcat

Expected tags were not observed during the run:

- `MainActivity`
- `WatchdogService`
- `StandardScanner`
- `ScannerChain`

This remains true even when the app is visibly running and `WatchdogService` is confirmed via `dumpsys`.

Interpretation:

- The logging verification path is still broken on this hardware/OS combination.
- This is tracked separately in issue `#122`.

### 6. App UI remains minimal during active startup

`uiautomator dump` of the app UI during active runtime showed:

- title `wscan+`
- body text `Starting scanner...`

Interpretation:

- There is no user-visible error state exposed during the observed coarse-only failure.
- From an operator perspective, the app appears to be working while scanner acquisition is actually impaired.

### 7. WiFiAnalyzer confirms the tablet can render scan-related UI

WiFiAnalyzer launched successfully during the session and its UI hierarchy was captured locally.

Interpretation:

- The tablet is capable of running a third-party Wi-Fi analysis app in the same environment.
- That supports treating the wscan+ gaps as app/runtime-path issues rather than a total device inability to participate in Wi-Fi analysis.

### 8. Background-location fix removes the previous keyguard permission failure signature

With the updated build that adds:

- `ACCESS_BACKGROUND_LOCATION`
- `FOREGROUND_SERVICE_LOCATION`
- `WatchdogService` foreground type `location|dataSync`

Observed behavior during a backgrounded / screen-off sample:

- `WatchdogService` remained foreground
- `WifiService: getScanResults uid=10261` appeared for the app UID
- the prior keyguard-era signature did not recur:

```text
WifiService: Permission violation - getScanResults not allowed ... UID 10261 has no location permission
```

- shell `cmd wifi list-scan-results` on this specific capture still returned `No scan results`

Interpretation:

- The fix changes the app-access behavior on the Revvl in the expected direction.
- This does not fully resolve Revvl observability, because:
  - app-side logs are still weak on this device
  - this capture did not yield a populated shell scan list at the same moment
- Even so, the important regression signal is that the app no longer hits the prior background/keyguard permission boundary.

## Conclusions

1. The Revvl Tab 2 is a valid active test target, but it behaves differently from the previous Motorola baseline.
2. The app's no-permission guard is working.
3. The coarse-only fallback is incomplete on this Android 15 OEM build:
   - launch succeeds
   - service starts
   - scan retrieval still fails
4. Full fine location restores the current scan path.
5. The background-location / location-typed foreground-service fix removes the prior keyguard permission-denial signature on this device.
6. App-side log visibility through adb is still missing and remains the main Revvl-specific debugging gap.

## Follow-up items

- `#122` - missing visible app-side adb logs on Revvl Android 15
- `#124` - coarse-only fallback remains operationally broken on Revvl Android 15
- `#125` - backgrounded / keyguard scan path loses effective location access on Android 15
