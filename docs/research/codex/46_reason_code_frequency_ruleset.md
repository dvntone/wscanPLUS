# Reason?Code Frequency Ruleset (Verified Meanings Only)

**Date**: 2026-03-19 (America/Los_Angeles)

## Purpose
Provide a **non?speculative** ruleset that ties 802.11 reason codes to **frequency patterns** without claiming specific attacks. This uses only verified reason?code meanings and avoids guessing.

## Verified Reason?Code Meanings (Reference)
From `ieee802_11_defs.h` (wpa_supplicant/hostapd):
- `1` Unspecified
- `2` Previous authentication no longer valid
- `3` Deauth leaving
- `4` Disassoc due to inactivity
- `5` Disassoc AP busy
- `6` Class 2 frame from non?auth STA
- `7` Class 3 frame from non?assoc STA
- `8` Disassoc STA has left
- `9` STA req assoc without auth
- `10` Power capability not valid
- `11` Supported channel not valid
- `13` Invalid IE
- `14` MIC failure
- `15` 4?way handshake timeout
- `16` Group key update timeout
- `17` IE in 4?way differs
- `18` Group cipher not valid
- `19` Pairwise cipher not valid
- `20` AKMP not valid
- `21` Unsupported RSN IE version
- `22` Invalid RSN IE capabilities
- `23` 802.1X auth failed
- `24` Cipher suite rejected
- `34` Disassoc low ACK
- `52?59` Mesh?related reasons

Source: wpa_supplicant/hostapd `ieee802_11_defs.h`. ?cite?turn1view0?

## Frequency?Based Rules (No Attack Attribution)
These rules only elevate **confidence of anomalous behavior** based on repetition and scope. They do not label an attack.

### A) Single?BSSID Repetition
- **If the same BSSID shows the same reason code ? 3 times within 10 minutes**, mark as **persistent anomaly** for that BSSID.
- Rationale: repeated identical failures are more meaningful than a single drop.

### B) Multi?BSSID Spread
- **If ? 3 different BSSIDs produce the same reason code within 10 minutes**, mark as **environment/systemic anomaly**.
- Rationale: broad repetition suggests a systemic issue (client or RF environment) rather than one AP.

### C) Crypto/Auth Cluster (Reason Codes 14?24)
- If any of the **crypto/auth reason codes** (14?24) recur ? 2 times within 10 minutes for the same SSID/BSSID, mark as **auth?path anomaly**.
- This is a **signal class** only, not an attack label.

### D) Inactivity vs. Busy vs. Low ACK
- Reason `4` (inactivity), `5` (AP busy), `34` (low ACK) should only be treated as **low?confidence anomalies** unless repeated.
- Apply rule A or B before escalating.

### E) Unsupported Capability/Channel (10, 11, 21, 22)
- If these occur repeatedly, mark as **capability mismatch anomaly** (likely configuration or compatibility).
- Do **not** label as attack without additional signals.

## Evidence Handling
- Each anomaly record should include: reason code, BSSID, SSID, timestamp(s), RSSI, and count within window.
- These records feed the `anomaly_candidate` table and require additional corroboration before an `alert_event`.

## Notably Excluded (To Avoid Guessing)
- No direct mapping of a reason code to ?deauth attack? or ?evil twin.?
- No claims about intent ? only observable repetition and scope.

## Reference Note: Status vs Reason Codes
- Status codes (e.g., `WLAN_STATUS_UNSUPPORTED_RSN_IE_VERSION` = 44) are **not** reason codes and should not be treated as disconnect reasons. ?cite?turn1view0?
