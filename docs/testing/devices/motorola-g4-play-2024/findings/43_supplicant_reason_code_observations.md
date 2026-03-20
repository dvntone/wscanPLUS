# Supplicant Deauth/Disconnect Reason Codes (Observed)

**Date**: 2026-03-19 (America/Los_Angeles)

## Purpose
Document whether wpa_supplicant emits reason?code lines that can indicate deauth/disconnect events when monitored via ADB.

## Source
- `docs\codex_findings_gaps\wscan_toggle_logcat.txt`

## Observed Lines (Redacted)
From `wpa_supplicant` logs:
- `Request to deauthenticate ... reason=3 (DEAUTH_LEAVING)`
- `Event DEAUTH (11) received`
- `Deauthentication notification`
- `CTRL-EVENT-DISCONNECTED ... reason=3 locally_generated=1 disconnect_rssi=...`

## Interpretation (verified)
- The device **does emit** deauth/disconnect reason?code lines in logcat.
- These can be monitored from a desktop app using `adb logcat` filters.

## Notes
- This evidence comes from system logs, not a public Android SDK callback.
- We have not yet validated whether WifiScanner API error codes (public SDK) expose equivalent reason codes on this device.
