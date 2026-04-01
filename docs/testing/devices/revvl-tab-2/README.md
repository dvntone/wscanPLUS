# Revvl Tab 2 Test Prep

Current active Android tablet target for on-device verification.

## Device

- Product model from ADB: `9185W`
- Marketing name: `Revvl Tab 2`
- Android version: `15`

## Verified preflight state

- Developer mode enabled
- `WiFiAnalyzer` installed as `com.vrem.wifianalyzer` (`versionName=3.2.2`)
- `com.wscanplus.app` is now installed for current-main validation
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

**Updated 2026-04-01 — Phase 3 complete. New session required.**

Phase 3 is fully merged. The next session should validate the Phase 3 additions on top of the Phase 2 baseline already confirmed in the March session.

### Phase 3 changes to verify on-device

- **DB encryption** — `wscan.db` is now encrypted with SQLCipher AES-256. Confirm via `adb pull` that the file is not readable by sqlite3 without the passphrase.
- **DB passphrase generation** — on first launch after update, logcat should emit `DB passphrase generated; existing plaintext DB removed if present`. On subsequent launches, no passphrase log should appear.
- **Retention purge** — logcat should emit `Retention purge: N session(s) older than 30d removed` on startup. Zero is expected on a fresh install.
- **CTI cache prune** — logcat should emit `CTI cache pruned` on startup.
- **Consent gate** — opt-in dialog must appear on first use of any CTI/network feature. Verify CrowdSec lookups are blocked until consent is given.
- **Google Maps heatmap** — `ScanMapActivity` should launch and render GPS-tagged scan results on the heatmap. Verify GPS tags are present in logcat during scanning.
- **Degraded mode** — verify service starts in degraded mode when background location is not granted.

### Phase 2 baseline (confirmed 2026-03-20 — no re-verification needed unless regressions observed)

- app-side logging, coarse-location fallback, sticky restart permission re-check, stale scan filtering, USB→standard chain fallback

## Latest baseline result

- See `2026-03-20-baseline-smoke-test.md` for the first Revvl Tab 2 run against the current debug build.
- See `2026-03-20-full-adb-matrix.md` for the broader Wi-Fi, location, permissions, and runtime validation pass.
- The latest live re-test on current `main` is summarized in the same full matrix note and narrows the remaining concerns to:
  - secure-lockscreen / stricter background validation
  - repeatable app-log visibility procedure
  - issue/docs alignment for coarse-only behavior

## Suggested ADB checks

```powershell
adb -s <redacted-device> logcat -c
adb -s <redacted-device> shell pm list packages | Select-String "com.wscanplus.app|com.vrem.wifianalyzer"
adb -s <redacted-device> shell dumpsys activity services com.wscanplus.app | Select-String -Pattern "WatchdogService|foreground" -Context 0,2
adb -s <redacted-device> logcat -d | Select-String -Pattern "MainActivity|WatchdogService|StandardScanner|ScannerChain"
```

## WiFiAnalyzer comparison note

ADB cannot directly read WiFiAnalyzer's rendered scan list, but it can confirm the package, launch state, and UI hierarchy captures if needed during the session. Treat WiFiAnalyzer as an operator-visible reference point, not an API source.
