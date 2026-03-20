# Claude Briefing (Non-Coding Handoff)

**Date**: 2026-03-19 (America/Los_Angeles)

## What Exists
A full verified findings set is under:
- `docs/research/codex/` for analysis and durable research notes
- `docs/testing/devices/` for device-specific evidence summaries

Key documents:
- `docs/testing/shared/47_verification_status.md` - which docs are observed vs spec vs policy; includes the Claude re-verify note.
- `docs/testing/devices/motorola-g4-play-2024/findings/18_wifi_scan_off_behavior.md` - Wi-Fi scan works with Wi-Fi off only if scan-always is enabled.
- `docs/testing/devices/motorola-g4-play-2024/findings/21_altitude_location_findings.md` + `docs/testing/devices/motorola-g4-play-2024/findings/22_altitude_stability_sampling.md` - altitude available but stale without active updates.
- `docs/testing/devices/motorola-g4-play-2024/findings/26_mock_altitude_capture.md` - FakeGPS Route mock location injects altitude; OS marks as mock.
- `docs/testing/devices/motorola-g4-play-2024/findings/28_dev_build_permissions_runtime.md` - dev build already has runtime perms + WatchdogService foreground.
- `docs/testing/devices/motorola-g4-play-2024/findings/33_wigle_export_schema.md` + `docs/testing/devices/motorola-g4-play-2024/findings/34_wigle_m8b_export.md` - WiGLE CSV/KML schema + m8b format spec.
- `docs/testing/devices/motorola-g4-play-2024/findings/43_supplicant_reason_code_observations.md` - wpa_supplicant reason codes visible in logcat.
- `docs/research/codex/44_wifiscanner_error_codes.md` - WifiScanner failure codes are scan errors, not disconnect reasons.
- `docs/research/codex/45_reason_codes_reference.md` + `docs/research/codex/46_reason_code_frequency_ruleset.md` - verified reason meanings + frequency rules.
- `docs/research/codex/42_minimal_retention_schema.md` + `docs/research/codex/41_evidence_anomaly_retention.md` - retention required for anomaly detection; false-positive-safe schema.

## Verified Signals (ADB/logcat)
- wpa_supplicant emits `CTRL-EVENT-DISCONNECTED` with reason codes in logcat.
- Scan results can be pulled with Wi-Fi off if `wifi_scan_always_enabled=1`.
- Altitude exists in fused/network providers; mock location proves altitude injection.

## Non-Coding Requests Pending
- App-side BLE scanning/logging (ADB alone cannot capture BLE scans).
- App-side location updates to avoid stale altitude.

## WSCAN+ Dev Build
- Package: `com.wscanplus.app` (versionName 0.0.1, targetSdk 36).
- Foreground service running: `WatchdogService`.
- No app-emitted logs detected (needs explicit tags).

## Rules
- If any evidence here conflicts with current behavior, re-verify and update it (see `docs/testing/shared/47_verification_status.md`).
