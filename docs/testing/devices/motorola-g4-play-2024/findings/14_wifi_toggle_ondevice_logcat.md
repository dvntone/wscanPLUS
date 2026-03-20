# Wi-Fi Toggle Capture (On-Device Logcat File)

Created: 2026-03-19
Device: <redacted-device>
Scope: On-device logcat capture while Wi-Fi was toggled via adb

## Commands Run

- adb shell sh -c "logcat -c; logcat -v time -b main,system,events,radio -f /sdcard/wscan_toggle_logcat.txt &"
- adb shell svc wifi disable
- adb shell svc wifi enable
- adb shell pkill logcat
- adb pull /sdcard/wscan_toggle_logcat.txt C:\Users\Devia\Documents\GitHub\wscanplus\docs\codex_findings_gaps\wscan_toggle_logcat.txt

## File Location

- C:\Users\Devia\Documents\GitHub\wscanplus\docs\codex_findings_gaps\wscan_toggle_logcat.txt

## Observed In File

- Wifi HAL and wpa_supplicant log lines present
- ClientModeImpl and ConnectivityService log lines present
- WifiScoreCard and link-layer stats logs present

## Not Observed In File

- Explicit Wi-Fi state transition lines (disable or enable)
- DISASSOC or DEAUTH reason code lines

## Notes

- The log file includes SSID, BSSID, and IP details from system logs
- This is a point-in-time capture tied to the Wi-Fi toggle event
- Further captures may be needed for deauth or disassoc reason codes

