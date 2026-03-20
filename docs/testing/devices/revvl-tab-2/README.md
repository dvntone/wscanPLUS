# Revvl Tab 2 Test Prep

Current active Android tablet target for on-device verification.

## Device

- Product model from ADB: `9185W`
- Marketing name: `Revvl Tab 2`
- Android version: `15`

## Verified preflight state

- Developer mode enabled
- `WiFiAnalyzer` installed as `com.vrem.wifianalyzer` (`versionName=3.2.2`)
- `com.wscanplus.app` not yet installed on this device at prep time
- `settings get global wifi_scan_always_enabled` returned `0`
- `settings get global wifi_scan_throttle_enabled` returned `null`, so OEM behavior must be verified from runtime results rather than that flag alone

## First-pass test sequence

1. Install the latest debug APK built from current `main`.
2. Grant location permissions and confirm app-side logcat emits:
   - `MainActivity`
   - `WatchdogService`
   - `StandardScanner`
   - `ScannerChain`
3. Compare visible scan behavior with `WiFiAnalyzer` on the same device.
4. Verify Android 15 service behavior:
   - foreground notification present
   - scanner starts only with location permission
   - coarse-only permission path behaves as expected if exercised
5. Capture new raw evidence under this device's local `artifacts/` path and summarize conclusions in markdown.

## Current development-phase focus

This device session should validate the app as it exists after the recent Phase 2 and bug-fix merges:

- app-side logging added for adb verification
- coarse-location fallback in `MainActivity`
- `WatchdogService` runtime permission re-check for sticky restarts
- stale scan-result filtering in `StandardScanner`
- scanner-chain USB fallback to standard scanning

The first session goal is not full heuristic validation in the field. It is to confirm that the current build starts, logs correctly, handles permissions correctly, and produces scan/runtime signals that can support later threat-intelligence testing.

## Latest baseline result

- See `2026-03-20-baseline-smoke-test.md` for the first Revvl Tab 2 run against the current debug build.
- See `2026-03-20-full-adb-matrix.md` for the broader Wi-Fi, location, permissions, and runtime validation pass.

## Suggested ADB checks

```powershell
adb -s <redacted-device> logcat -c
adb -s <redacted-device> shell pm list packages | Select-String "com.wscanplus.app|com.vrem.wifianalyzer"
adb -s <redacted-device> shell dumpsys activity services com.wscanplus.app | Select-String -Pattern "WatchdogService|foreground" -Context 0,2
adb -s <redacted-device> logcat -d | Select-String -Pattern "MainActivity|WatchdogService|StandardScanner|ScannerChain"
```

## WiFiAnalyzer comparison note

ADB cannot directly read WiFiAnalyzer's rendered scan list, but it can confirm the package, launch state, and UI hierarchy captures if needed during the session. Treat WiFiAnalyzer as an operator-visible reference point, not an API source.

