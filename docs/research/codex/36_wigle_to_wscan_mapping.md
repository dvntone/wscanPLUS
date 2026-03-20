# WiGLE Export ? WSCAN+ Data Contract Mapping

**Date**: 2026-03-19 (America/Los_Angeles)

## Purpose
Map WiGLE CSV/KML export fields to the current WSCAN+ data contract so Claude can align schemas without guessing.

## WiGLE CSV Fields (Observed)
From `WigleWifi_*.csv.gz` header:
- `MAC`
- `SSID`
- `AuthMode`
- `FirstSeen`
- `Channel`
- `RSSI`
- `CurrentLatitude`
- `CurrentLongitude`
- `AltitudeMeters`
- `AccuracyMeters`
- `Type`

## Mapping to WSCAN+ (Proposed)
- `MAC` ? `bssid`
- `SSID` ? `ssid`
- `AuthMode` ? `auth_mode` or `security`
- `FirstSeen` ? `first_seen` (timestamp)
- `Channel` ? `channel`
- `RSSI` ? `rssi`
- `CurrentLatitude` ? `latitude`
- `CurrentLongitude` ? `longitude`
- `AltitudeMeters` ? `altitude_m`
- `AccuracyMeters` ? `accuracy_m`
- `Type` ? `signal_type` (e.g., WIFI)

## Gaps vs WSCAN+ Contract
- WiGLE CSV does **not** include:
  - BSSID vendor/OUI
  - Wi?Fi standard (802.11n/ac/ax)
  - Frequency (unless inferred from channel)
  - Scan duration / scan source / scan sequence id
  - Device metadata (model, OS, app build)

## Notes
- WiGLE KML includes a richer per?network description (capabilities, frequency, timestamp) that can optionally be parsed if you need it.
- Use WiGLE CSV as a **baseline** for interoperability, not a ceiling.
