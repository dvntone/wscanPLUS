# Claude Briefing (Non?Coding Handoff)

**Date**: 2026-03-19 (America/Los_Angeles)

## What Exists
A full verified findings set is under:
- `docs/codex_findings_gaps/`

Key documents:
- `47_verification_status.md` ? which docs are observed vs spec vs policy; includes **Claude re?verify note**.
- `18_wifi_scan_off_behavior.md` ? Wi?Fi scan works with Wi?Fi off **only if** scan?always enabled.
- `21_altitude_location_findings.md` + `22_altitude_stability_sampling.md` ? altitude available but stale without active updates.
- `26_mock_altitude_capture.md` ? FakeGPS Route mock location injects altitude; OS marks as mock.
- `28_dev_build_permissions_runtime.md` ? dev build already has runtime perms + WatchdogService foreground.
- `33_wigle_export_schema.md` + `34_wigle_m8b_export.md` ? WiGLE CSV/KML schema + m8b format spec.
- `43_supplicant_reason_code_observations.md` ? wpa_supplicant reason codes visible in logcat.
- `44_wifiscanner_error_codes.md` ? WifiScanner failure codes are scan errors, not disconnect reasons.
- `45_reason_codes_reference.md` + `46_reason_code_frequency_ruleset.md` ? verified reason meanings + frequency rules (no attack labels).
- `42_minimal_retention_schema.md` + `41_evidence_anomaly_retention.md` ? retention required for anomaly detection; FP?safe schema.

## Verified Signals (ADB/logcat)
- wpa_supplicant emits `CTRL-EVENT-DISCONNECTED` with reason codes in logcat.
- Scan results can be pulled with Wi?Fi off if `wifi_scan_always_enabled=1`.
- Altitude exists in fused/network providers; mock?location proves altitude injection.

## Non?Coding Requests Pending
- App?side BLE scanning/logging (ADB alone cannot capture BLE scans).
- App?side location updates to avoid stale altitude.

## WSCAN+ Dev Build
- Package: `com.wscanplus.app` (versionName 0.0.1, targetSdk 36).
- Foreground service running: `WatchdogService`.
- No app?emitted logs detected (needs explicit tags).

## Rules
- If any evidence here conflicts with current behavior, **re?verify and update** (see `47_verification_status.md`).
