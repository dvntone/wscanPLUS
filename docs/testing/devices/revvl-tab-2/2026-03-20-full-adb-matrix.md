# Revvl Tab 2 Full ADB Matrix

**Date:** 2026-03-20  
**Device:** `Revvl Tab 2` (`9185W`)  
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

## Conclusions

1. The Revvl Tab 2 is a valid active test target, but it behaves differently from the previous Motorola baseline.
2. The app's no-permission guard is working.
3. The coarse-only fallback is incomplete on this Android 15 OEM build:
   - launch succeeds
   - service starts
   - scan retrieval still fails
4. Full fine location restores the current scan path.
5. App-side log visibility through adb is still missing and blocks the intended debugging workflow.

## Follow-up items

- `#122` - missing visible app-side adb logs on Revvl Android 15
- new issue needed for coarse-only fallback remaining operationally broken on Revvl Android 15
