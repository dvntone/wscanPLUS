# Wi-Fi Toggle Logcat Capture (Focused)

Created: 2026-03-19
Device: ZY22KFCNSK
Scope: Focused logcat capture after manual Wi-Fi toggle

## Action Performed

- User manually toggled Wi-Fi off and then on

## Command Run (After Toggle)

adb logcat -d -v brief -b main,system,events,radio | Select-String -Pattern "wpa_supplicant|Wifi|Wi-Fi|Supplicant|EAP|AUTH|DISASSOC|DEAUTH|svc wifi|WifiManager|ConnectivityService" -Context 0,2

## Observed (In This Capture)

- wpa_supplicant log lines present
- Wifi HAL and client mode logs present
- WifiScoreCard and throughput logs present
- ConnectivityService logs present

## Not Observed (In This Capture)

- No explicit Wi-Fi disable/enable state transitions
- No explicit DISASSOC or DEAUTH reason codes

## Implication

- The device exposes Wi-Fi subsystem logs to adb logcat
- This capture did not include explicit toggle events; a broader or timed capture may be required

## Follow-Up Options (Read-Only)

- Capture full logcat with timestamp and then filter offline
- Run dumpsys wifi immediately after toggle to confirm state transitions

## Notes

- This is a point-in-time sample only
- The absence of toggle logs does not mean they are unavailable; it means they did not appear in this capture
