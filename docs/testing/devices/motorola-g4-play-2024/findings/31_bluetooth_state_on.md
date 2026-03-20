# Bluetooth State (Enabled)

**Date**: 2026-03-19 (America/Los_Angeles)

## Commands
```powershell
adb shell svc bluetooth enable
adb shell settings get global bluetooth_on
adb shell dumpsys bluetooth_manager | Select-String -Pattern 'enabled|state|BLE|LE' -Context 0,1
```

## Observed (verified)
- `bluetooth_on = 1`
- `enabled: true`
- `state: ON`
- BLE subsystem started (`BleOnState` ? `OnState`)

## Notes
- The bluetooth manager output contains **device names and MAC addresses**; those are sensitive and were not recorded in this doc.

## Implication
- BLE scanning is now possible **if** the app implements it, but ADB still cannot enumerate BLE scan results without app?side code.
