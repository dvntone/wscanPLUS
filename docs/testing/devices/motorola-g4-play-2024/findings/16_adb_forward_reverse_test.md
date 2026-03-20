# ADB Forward and Reverse Test (Ports)

Created: 2026-03-19
Device: ZY22KFCNSK
Scope: Verify adb forward and reverse behavior for app transport

## Commands Run

- adb forward --list
- adb reverse --list
- adb forward tcp:9000 tcp:9000
- adb reverse tcp:9001 tcp:9001
- adb forward --list
- adb reverse --list
- adb forward --remove tcp:9000
- adb reverse --remove tcp:9001

## Verified Results

- Forward mapping created successfully for tcp:9000
- Reverse mapping created successfully for tcp:9001
- Forward and reverse listings showed expected mappings
- Mappings removed successfully

## Implication

- ADB port forwarding is functional for the device and can be used for desktop <-> Android transport
