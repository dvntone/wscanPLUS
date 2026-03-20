# App Code Review ? Verified Improvements (No Changes)

**Date**: 2026-03-19 (America/Los_Angeles)

## Scope
Android app/core code review with **verified** suggestions only (grounded in current code and ADB findings). No code changes made.

## Suggestions (Verified by Code + Observed Behavior)

### 1) Expose `wifi_scan_always_enabled` State in App UI/Logs
**Why (verified)**:
- ADB tests show scans work with Wi?Fi off **only** when `wifi_scan_always_enabled=1`.
- This gating is critical to scan reliability.

**Suggestion**:
- Read and surface `wifi_scan_always_enabled` in app status/logs so users understand why scans may be empty when Wi?Fi is off.

---

### 2) Request Fresh Location Updates When Altitude Matters
**Why (verified)**:
- `dumpsys location` altitude values were **stale** without active updates (`22_altitude_stability_sampling.md`).

**Suggestion**:
- Use active location requests when altitude is needed, rather than relying on last?known values.

---

### 3) Split App vs Desktop Responsibilities for Supplicant Reason Codes
**Why (verified)**:
- wpa_supplicant reason codes are visible in logcat via ADB (`43_supplicant_reason_code_observations.md`).
- There is **no public SDK** reason?code callback; WifiScanner error codes are scan failures only (`44_wifiscanner_error_codes.md`).

**Suggestion**:
- Treat reason?code monitoring as a **desktop/ADB feature**, not an in?app feature.

---

### 4) Permission Revocation Safety (Service Restart)
**Why (verified)**:
- `WatchdogService` can restart via `START_STICKY` after permissions are revoked.

**Suggestion**:
- Add permission checks inside `WatchdogService` before starting scanners; stop gracefully if missing.

---

## References
- `18_wifi_scan_off_behavior.md`
- `22_altitude_stability_sampling.md`
- `43_supplicant_reason_code_observations.md`
- `44_wifiscanner_error_codes.md`
