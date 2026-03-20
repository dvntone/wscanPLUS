# 802.11 Reason Codes (Verified Reference)

**Date**: 2026-03-19 (America/Los_Angeles)

## Purpose
Provide a verified, non?speculative mapping of **802.11 reason codes** seen in `wpa_supplicant` logcat lines. This is a **reference only**; it does not claim attack attribution.

## Reason Codes (from wpa_supplicant/hostapd defs)
The following reason codes and meanings are defined in `ieee802_11_defs.h`:
- `1` ? Unspecified
- `2` ? Previous authentication no longer valid
- `3` ? Deauth leaving
- `4` ? Disassoc due to inactivity
- `5` ? Disassoc AP busy
- `6` ? Class 2 frame from non?auth STA
- `7` ? Class 3 frame from non?assoc STA
- `8` ? Disassoc STA has left
- `9` ? STA req assoc without auth
- `10` ? Power capability not valid
- `11` ? Supported channel not valid
- `13` ? Invalid IE
- `14` ? MIC failure
- `15` ? 4?way handshake timeout
- `16` ? Group key update timeout
- `17` ? IE in 4?way differs
- `18` ? Group cipher not valid
- `19` ? Pairwise cipher not valid
- `20` ? AKMP not valid
- `21` ? Unsupported RSN IE version
- `22` ? Invalid RSN IE capabilities
- `23` ? 802.1X auth failed
- `24` ? Cipher suite rejected
- `34` ? Disassoc low ACK
- `52?59` ? Mesh?related reasons (peering cancelled, max peers, policy violation, close received, retries, confirm timeout, invalid GTK, inconsistent params)

## Important Clarification: Reason vs Status Codes
- **Reason code 44 is not defined** in the reason?code list above.
- `44` is defined as a **STATUS** code (`WLAN_STATUS_UNSUPPORTED_RSN_IE_VERSION`), not a reason code. Do not treat status code 44 as a disconnect reason.

## What This Means for Detection
- Reason codes describe *why* a deauth/disassoc happened, but they **do not label attacks**.
- Attack attribution requires additional signals (frequency, context, RSSI behavior, multiple indicators).

## Sources
- wpa_supplicant/hostapd `ieee802_11_defs.h` reason code definitions.
- wpa_supplicant/hostapd status code definitions (status code 44).
