# WifiScanner Error Codes vs Disconnect Reasons

**Date**: 2026-03-19 (America/Los_Angeles)

## Purpose
Clarify which **default error codes** come from the WifiScanner API, and whether they indicate disconnect/deauth events.

## WifiScanner Reason Codes (AOSP)
From `android.net.wifi.WifiScanner` source:
- `REASON_SUCCEEDED = 0`
- `REASON_UNSPECIFIED = -1`
- `REASON_INVALID_LISTENER = -2`
- `REASON_INVALID_REQUEST = -3`
- `REASON_NOT_AUTHORIZED = -4`
- `REASON_DUPLICATE_REQEUST = -5`
- `REASON_BUSY = -6`
- `REASON_ABORT = -7`
- `REASON_NO_DEVICE = -8`
- `REASON_INVALID_ARGS = -9`
- `REASON_TIMEOUT = -10`

## Important Distinction (Verified)
- WifiScanner reason codes describe **scan request failures**, not deauth/disconnect events.
- Deauth/disconnect reason codes are observed in **wpa_supplicant logcat** (see `43_supplicant_reason_code_observations.md`).

## WiFiManager Supplicant Errors (Deprecated)
- Android?s WifiManager exposes `EXTRA_SUPPLICANT_ERROR` / `ERROR_AUTHENTICATING`, but these are deprecated in API 28.
