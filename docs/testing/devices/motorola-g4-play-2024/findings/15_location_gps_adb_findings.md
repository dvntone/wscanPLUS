# Location and GPS State (ADB)

Created: 2026-03-19
Device: <redacted-device>
Scope: Read-only location and GPS state checks

## Commands Run

- adb shell settings get secure location_mode
- adb shell settings get secure location_providers_allowed
- adb shell dumpsys location

## Verified Results

Location mode
- location_mode: 3

Providers
- passive provider: enabled, last location present
- network provider: enabled, last location present
- fused provider: enabled, last location present
- gps provider: enabled, last location null, mStarted=false

Recent delivery events (from dumpsys location)
- network provider delivered location events
- fused provider delivered location events
- passive provider delivered location events

## Notes

- location_providers_allowed returned null on this device
- Location data in dumpsys includes coordinates, accuracy, and timestamps
- This output confirms that location signals are active and observable via adb

