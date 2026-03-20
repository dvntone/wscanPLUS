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

### 3. Original coarse-only finding from the first Revvl pass

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

- This was the observed behavior during the earlier Revvl pass.
- Treat it as historical evidence only until compared against current `main`.

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

### 4a. Original background/keyguard finding from the first Revvl pass

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

- This was the observed behavior during the earlier Revvl pass.
- Treat it as historical evidence only until compared against current `main`.

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

### 8. Historical note from the earlier permission/service fix pass

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

- This was an earlier positive signal, but it was not enough on its own to close the Revvl questions.
- Treat it as historical evidence only until compared against current `main`.

### 9. Current-main re-test on 2026-03-20

Environment for the re-test:

- current build installed from local `main`: `versionCode=2`, `versionName=0.1.0`
- Developer mode enabled
- screen unlocked for the clean foreground log pass
- no secure lockscreen configured on the device

Observed behavior with full-fine permissions:

- `MainActivity` launches successfully
- `WatchdogService` starts as a foreground service
- app-side debug tags are visible in `adb logcat` when the device is unlocked and the app is foregrounded:
  - `D/WatchdogService: Service created`
  - `D/WatchdogService: Service started`
  - `D/ScannerChain: Using Standard scanner`
  - `D/StandardScanner: Scanner started (API 35)`

Observed behavior after `HOME` and after screen-off in the non-secure-lockscreen scenario:

- `WatchdogService` remained foreground
- the old Revvl signature

```text
WifiService: Permission violation - getScanResults not allowed ... UID has no location permission
```

was not reproduced in that non-secure pass

Observed behavior in coarse-only on current `main`:

- `ACCESS_FINE_LOCATION=false`
- `ACCESS_COARSE_LOCATION=true`
- `ACCESS_BACKGROUND_LOCATION=true`
- `NEARBY_WIFI_DEVICES=true`
- `MainActivity` still launches
- `WatchdogService` does not start

Interpretation:

- On current `main`, coarse-only no longer reaches the old “service starts, then scan retrieval fails” state on this device.
- The remaining background question on Revvl is now the stronger secure-lockscreen / stricter background case, not the basic non-secure HOME or screen-off path.
- The logging issue is narrower than originally phrased: app tags are visible when the device is truly unlocked and the app is on the successful foreground path.

## Conclusions

1. The Revvl Tab 2 is a valid active test target, but it behaves differently from the previous Motorola baseline.
2. The app's no-permission guard is working.
3. The original coarse-only and background/keyguard findings must now be read as historical evidence, not final current-main behavior.
4. Current `main` blocks coarse-only before `WatchdogService` startup on this device.
5. The non-secure Revvl background re-test did not reproduce the earlier permission-denial signature on current `main`.
6. App-side adb logging is state-sensitive on this device, not completely absent; the clean unlocked foreground path now shows the expected app tags.

## Follow-up items

- `#122` - narrow the issue to state-sensitive log visibility / repeatable operator procedure on Revvl Android 15
- `#124` - align issue/docs state with current `main`, which now blocks coarse-only before service startup
- `#125` - re-test the stronger secure-lockscreen background case on current `main`
