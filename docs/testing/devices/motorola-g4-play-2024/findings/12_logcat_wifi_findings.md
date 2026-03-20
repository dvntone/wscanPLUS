# ADB Logcat Findings (Wi-Fi and Supplicant Logs)

Created: 2026-03-19
Device: <redacted-device>
Scope: Read-only logcat sampling for Wi-Fi and supplicant visibility

## Command Run

adb shell logcat -d -v brief -b main,system,events,radio | Select-String -Pattern "wpa_supplicant|Wifi|Wi-Fi|Supplicant|EAP|AUTH|DISASSOC|DEAUTH" -Context 0,2

## What Worked

- Logcat returned multiple lines from wpa_supplicant and Wifi HAL
- Example observed tags include:
  - wpa_supplicant
  - WifiHAL
  - cnss-daemon
  - LOWI-Scan

## What Was Not Observed (In This Sample)

- Explicit DISASSOC or DEAUTH reason code lines were not present in this capture window
- No EAP auth failure lines were present in this capture window

## Implication

- The device exposes wpa_supplicant log lines to adb logcat without additional configuration
- This is a viable data source for advanced detection paths in the current non-Play-Store posture

## Notes

- This is a point-in-time sample only
- Additional captures during active events will be required to verify reason codes and error detail availability

