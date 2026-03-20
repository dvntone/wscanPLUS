# Logcat Snapshot While Wi?Fi Off (Scan?Always On)

**Date**: 2026-03-19 (America/Los_Angeles)
**Log file**: `wscan_scanoff_logcat_20260319_080549.txt`

## Context
Wi?Fi was **disabled** and `wifi_scan_always_enabled=1`. We captured a logcat snapshot to see what Wi?Fi related signals are emitted when scanning remains possible without Wi?Fi connectivity.

**Environment note**: A VPN was enabled at the time of capture. This can affect connectivity stack behavior and should be considered when interpreting network/route?related logs.

## Commands
```powershell
adb logcat -d > docs\codex_findings_gaps\wscan_scanoff_logcat_20260319_080549.txt
Select-String -Path docs\codex_findings_gaps\wscan_scanoff_logcat_20260319_080549.txt -Pattern 'Wifi|wificond|wpa_supplicant|Supplicant|Scan|SCAN|ClientModeImpl|ConnectivityService|NetworkAgent|WifiService|WIFI'
```

## Notable Signals (examples found)
These appeared while Wi?Fi was off but scan?always was enabled:
- `wificond: Scan aborted`
- `ClientModeImpl.sendNetworkChangeBroadcast`
- `android.hardware.wifi@1.0-service: Wifi HAL stopped / started`
- `android.hardware.wifi@1.0-service: No active wlan interfaces in use! Using default`
- `wificond: Unsubscribe scan result for interface index...`

## Interpretation (verified)
- Even with Wi?Fi off, the system still exercises the Wi?Fi stack (HAL, wificond) when scan?always is enabled.
- Logcat does **not** include SSIDs/BSSIDs by default in this snapshot, but scan results remain accessible via `cmd wifi list-scan-results`.

## Integration Notes
- Reactive triggers can watch for wificond / HAL state transitions to detect scan behavior changes.
- Use this alongside `cmd wifi list-scan-results` for proactive scan data.

## Raw Data
See `docs\codex_findings_gaps\wscan_scanoff_logcat_20260319_080549.txt` for the full capture.
