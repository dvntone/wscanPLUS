# Bluetooth + Location Disabled (ADB)

**Date**: 2026-03-19 (America/Los_Angeles)

## Commands
```powershell
adb shell svc bluetooth disable
adb shell cmd location set-location-enabled false
adb shell settings get global bluetooth_on
adb shell cmd location is-location-enabled
```

## Observed (verified)
- Bluetooth: `0`
- Location enabled: `false`
