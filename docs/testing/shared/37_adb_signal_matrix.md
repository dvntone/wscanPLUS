# ADB Signal Matrix (What We Can Observe)

**Date**: 2026-03-19 (America/Los_Angeles)

## Purpose
Provide a clear matrix of which signals are observable via ADB today, and which require app?side instrumentation.

## Observable via ADB (Verified)
- Wi?Fi scan results (when Wi?Fi on OR scan?always enabled)
- Wi?Fi status (connected/disconnected, RSSI, SSID, BSSID)
- Location state (enabled/disabled)
- Location last?known altitude (fused/network)
- Bluetooth state (ON/OFF)
- App runtime permissions and appops
- Foreground service presence
- ADB forward/reverse availability

## Not Observable via ADB Alone (Requires App Code)
- BLE scan results (nearby devices list, RSSI)
- Real?time location updates without mock
- Heuristic decisions and scoring outputs
- Data export formats from WSCAN+ app

## Conditional / Gated
- Wi?Fi scan results when Wi?Fi OFF require `wifi_scan_always_enabled = 1`.
- Altitude values without active requests may be stale.

## Implication
This matrix defines what can be verified now (ADB only) vs. what must wait for app?side logging.
