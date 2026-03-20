# Logcat Filter Cookbook

**Date**: 2026-03-19 (America/Los_Angeles)

## Purpose
Provide the exact logcat filters we used and what each yields.

## Wi?Fi Stack Signals
```powershell
adb logcat -d | Select-String -Pattern 'wificond|wpa_supplicant|ClientModeImpl|WifiService|WIFI'
```
Yields:
- wificond scan events
- ClientModeImpl network change broadcasts
- Wi?Fi HAL start/stop messages

## App Package Mentions
```powershell
adb logcat -d | Select-String -Pattern 'com.wscanplus.app'
```
Yields:
- System/UI references to the app (WindowManager, ActivityTaskManager)

## Wi?Fi Off Scan Snapshot
```powershell
adb logcat -d > docs\codex_findings_gaps\wscan_scanoff_logcat_*.txt
```
Yields:
- Scan aborts
- HAL transitions

## Notes
- Logcat does **not** include scan results by default. Use `cmd wifi list-scan-results` for that.
- App?side logs are required to surface heuristics and BLE data.
