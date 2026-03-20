# WSCAN+ Dev Build Logcat Slice (No App Logs)

**Date**: 2026-03-19 (America/Los_Angeles)

## Purpose
Capture a minimal logcat slice tagged to `com.wscanplus.app` to see if the app emits logs by default.

## Commands
```powershell
adb logcat -c
adb logcat -d | Select-String -Pattern 'com.wscanplus.app'
```

## Observed (verified)
- Only system/UI lines referencing the package (WindowManager, IME, DynamicVolume).
- No app?emitted log tags or payloads were observed.

## Implication
App?side logging will be required to validate BLE, altitude, or heuristic events via logcat.
