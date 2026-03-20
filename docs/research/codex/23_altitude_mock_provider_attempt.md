# Altitude Live Update Attempt via ADB Test Provider

**Date**: 2026-03-19 (America/Los_Angeles)

## Goal
Attempt to force a live altitude update using the built?in `cmd location` test provider flow so we can validate floor?level changes without app code.

## Commands
```powershell
adb shell appops set shell android:mock_location allow
adb shell cmd location providers add-test-provider wscan_test --supportsAltitude
```

## Result (verified)
The add?test?provider step failed with:
```
java.lang.SecurityException: android from uid 2000 not allowed to perform MOCK_LOCATION
```

## Interpretation
- The `shell` user (uid 2000) cannot use mock location on this device, even after setting appops.
- This blocks use of ADB test providers for live altitude injection on this device.

## Implication
To validate altitude changes, we will need **app?side instrumentation** with mock?location permission (or a real GPS/fused update path) rather than relying on ADB alone.
