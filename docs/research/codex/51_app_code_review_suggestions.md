# App Code Review ? Suggested Fixes (No Changes)

**Date**: 2026-03-19 (America/Los_Angeles)

## Scope
Android app + core module code under:
- `android/app/src/main/...`
- `android/core/src/main/...`

This doc lists **suggested fixes/changes** based on **verified code behavior**. No code changes were made.

## Suggestions (Verified by Code)

### 1) Permission Handling: COARSE Location Fallback
**Where**:
- `android/app/src/main/kotlin/com/wscanplus/app/MainActivity.kt`
- `android/app/src/main/AndroidManifest.xml`

**Verified observation**:
- Manifest declares **both** `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` and notes the user may grant only COARSE.
- `MainActivity.requiredPermissions()` only requests **FINE** (and `NEARBY_WIFI_DEVICES` on API 33+).
- `hasAllPermissions()` requires all requested permissions, so **COARSE?only** grants will be treated as denial.

**Suggested fix**:
- Add COARSE to the runtime request set and allow **COARSE?only** as a degraded mode (if acceptable for your scanning logic).

**Reason**:
- This matches the manifest?s own guidance and prevents a false ?permission denied? state when the user chooses COARSE.

---

### 2) Guard Scanner Start After Permission Revocation
**Where**:
- `android/app/src/main/kotlin/com/wscanplus/app/WatchdogService.kt`

**Verified observation**:
- `WatchdogService` can restart via `START_STICKY` even if permissions were revoked.
- The service currently **assumes permissions** and calls `chain.start()`; TODO comment acknowledges this gap.

**Suggested fix**:
- Check required permissions inside `WatchdogService` before `startChain()`. If missing, stop the service and update the notification text.

**Reason**:
- Prevents crashes or silent failures when the OS restarts the service after permissions are revoked.

---

### 3) Handle Stale Scan Results Explicitly
**Where**:
- `android/core/src/main/kotlin/com/wscanplus/core/scanner/StandardScanner.kt`
- `android/core/src/main/kotlin/com/wscanplus/core/scanner/WifiScanResult.kt`

**Verified observation**:
- On API 30+, `registerScanResultsCallback()` delivers results **only when the system scans**.
- There is no explicit age check on `WifiScanResult.timestamp`.

**Suggested fix**:
- Record result age using `timestamp` (microseconds since boot) and **ignore/stale?flag** results older than a threshold.
- Optionally schedule periodic scan requests on older APIs (API < 28 uses `startScan()` once today).

**Reason**:
- Avoids treating stale results as fresh signals when the system scan cadence is low.

---

### 4) USB Scanner Fallback on Failure
**Where**:
- `android/core/src/main/kotlin/com/wscanplus/core/scanner/ScannerChain.kt`
- `android/core/src/main/kotlin/com/wscanplus/core/scanner/UsbScanner.kt`

**Verified observation**:
- `ScannerChain.start()` selects USB scanner if available; no fallback if USB start fails.

**Suggested fix**:
- Wrap `usbScanner.start()` in a try/catch and fallback to `standardScanner.start()` on failure.

**Reason**:
- Prevents total scan failure when an OTG adapter is connected but malfunctions.

---

### 5) Add App?Side Logging for Verification
**Where**:
- `android/app/src/main/kotlin/com/wscanplus/app/WatchdogService.kt`
- `android/core/src/main/kotlin/com/wscanplus/core/scanner/StandardScanner.kt`

**Verified observation**:
- ADB logcat shows **no app?emitted logs** (`30_dev_build_logcat_slice.md`).

**Suggested fix**:
- Add minimal structured logs (scan started/stopped, scan count, heuristic outputs) to enable field verification.

**Reason**:
- Without logs, desktop/ADB monitoring cannot validate scanner behavior or heuristics.

---

## Notes
- These suggestions are **grounded in current code** and documented behavior.
- Any changes should follow the ?one feature per session? rule and avoid conflicts with Claude?s pending changes.
