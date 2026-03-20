# Bluetooth State (ADB)

**Date**: 2026-03-19 (America/Los_Angeles)

## Commands
```powershell
adb shell settings get global bluetooth_on
adb shell dumpsys bluetooth_manager | Select-String -Pattern 'enabled|state|BLE|LE' -Context 0,1
```

## Observed (verified)
- `bluetooth_on = 0`
- `enabled: false`
- `state: OFF`
- `0 BLE apps registered`

## Implication
Bluetooth is currently OFF, so BLE scanning cannot run until Bluetooth is enabled.
