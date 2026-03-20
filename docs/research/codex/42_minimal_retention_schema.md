# Minimal Retention Schema (Low False?Positive Risk)

**Date**: 2026-03-19 (America/Los_Angeles)

## Purpose
Provide a minimal, evidence?grade retention schema that supports anomaly detection **without** inflating false positives.

## Design Principles (FP?Safe)
- **Retain raw evidence** but gate alerts on **repeated patterns**, not single events.
- Keep a clean separation between **observations** and **alerts**.
- Store confidence inputs (RSSI variance, time gaps) so heuristics can de?escalate noisy signals.

## Minimal Tables / Collections

### 1) `scan_observation`
Stores raw scan results (one row per network per scan).

Fields:
- `observation_id` (uuid)
- `device_id` (hash or stable identifier)
- `timestamp_utc` (ms)
- `ssid`
- `bssid`
- `rssi`
- `channel`
- `frequency_mhz` (optional; derive if available)
- `auth_mode` (if available)
- `scan_session_id` (uuid)
- `location_lat` (optional, coarse)
- `location_lng` (optional, coarse)
- `location_accuracy_m` (optional)
- `altitude_m` (optional)
- `source` (e.g., wifi_scan)

### 2) `scan_session`
Groups observations into a single scan sweep.

Fields:
- `scan_session_id` (uuid)
- `device_id`
- `timestamp_utc` (start)
- `duration_ms`
- `wifi_state` (on/off)
- `scan_always_enabled` (bool)
- `location_enabled` (bool)
- `battery_level` (optional)

### 3) `anomaly_candidate`
Stores computed signals **without** triggering alerts yet.

Fields:
- `candidate_id` (uuid)
- `timestamp_utc`
- `device_id`
- `bssid`
- `ssid`
- `score` (0?1)
- `reason_codes` (list)
- `evidence_window_start` / `evidence_window_end`
- `supporting_observation_ids` (list)

### 4) `alert_event`
Only created when confidence is high enough.

Fields:
- `alert_id` (uuid)
- `timestamp_utc`
- `device_id`
- `severity`
- `bssid`
- `ssid`
- `reason_codes`
- `evidence_window_start` / `evidence_window_end`
- `evidence_mode` (bool)

## False?Positive Controls (Required)
- Require **N confirmations** across **M minutes** before creating `alert_event`.
- Use **RSSI stability** thresholds to reduce transient spikes.
- Suppress alerts during **first?seen** period unless corroborated by multiple signals.
- Separate ?candidate? from ?alert? to allow human review or multi?signal corroboration.

## Notes
This schema supports retention **and** reduces false positives by preventing single?point evidence from becoming alerts.
