# Logcat Snapshot While Wi?Fi Off (Scan?Always On, VPN Off)

**Date**: 2026-03-19 (America/Los_Angeles)
**Log file**: `wscan_scanoff_novpn_logcat_20260319_080738.txt`

## Context
Wi?Fi was **disabled** and `wifi_scan_always_enabled=1`. VPN was **off** for this capture to remove VPN side?effects.

## Commands
```powershell
adb logcat -d > docs\codex_findings_gaps\wscan_scanoff_novpn_logcat_20260319_080738.txt
Select-String -Path docs\codex_findings_gaps\wscan_scanoff_novpn_logcat_20260319_080738.txt -Pattern 'Wifi|wificond|wpa_supplicant|Supplicant|Scan|SCAN|ClientModeImpl|ConnectivityService|NetworkAgent|WifiService|WIFI'
```

## Notable Signals (examples found)
- `BatteryExternalStatsWorker: WiFi energy data was reset ...`
- `wificond: Scan is not started. Ignore abort request`
- `ClientModeImpl.sendNetworkChangeBroadcast`
- `android.hardware.wifi@1.0-service: SetMacAddress succeeded on wlan0`

## Interpretation (verified)
- Wi?Fi stack signals are still emitted when scan?always is enabled and Wi?Fi is off.
- This capture is cleaner for network?stack analysis because VPN was off.

## Raw Data
See `docs\codex_findings_gaps\wscan_scanoff_novpn_logcat_20260319_080738.txt` for the full capture.
