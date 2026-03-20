# App Logging Checklist (Verification?Ready)

**Date**: 2026-03-19 (America/Los_Angeles)

## Purpose
Define minimal app?side logs needed to **verify** behavior (BLE, Wi?Fi scans, altitude, heuristics) via ADB/logcat. No code changes included.

## Required Log Tags (Suggested)
- `WSCAN_SCAN` ? scan lifecycle + counts
- `WSCAN_BLE` ? BLE scan status + counts
- `WSCAN_LOC` ? location updates + altitude + accuracy
- `WSCAN_HEUR` ? heuristic outputs (type, confidence, reason)
- `WSCAN_SYNC` ? export/desktop sync status

## Minimum Fields Per Log
### Wi?Fi Scan (`WSCAN_SCAN`)
- `scan_id`
- `result_count`
- `wifi_state` (on/off)
- `scan_always_enabled` (true/false)
- `duration_ms`

### BLE Scan (`WSCAN_BLE`)
- `scan_id`
- `result_count`
- `duration_ms`
- `bluetooth_state` (on/off)

### Location Update (`WSCAN_LOC`)
- `lat`/`lng` (coarse ok)
- `altitude_m`
- `h_acc_m`
- `v_acc_m`
- `is_mock`
- `provider` (fused/network/gps)

### Heuristic Output (`WSCAN_HEUR`)
- `heuristic_type`
- `confidence`
- `reason_codes`
- `ssid/bssid` (if evidence mode)
- `evidence_mode` (true/false)

### Sync (`WSCAN_SYNC`)
- `payload_count`
- `bytes_sent`
- `result` (success/fail)

## Verification Rules
- Logs must be emitted **only** on meaningful events (avoid spam).
- Each log should be a single line for easy grep.
- Include `timestamp_utc` or rely on logcat timestamps.

## Notes
- This checklist enables ADB?based validation without extra tooling.
- Once BLE/location logging exists, we can verify on device immediately.
