# Mock Location App Enabled (FakeGPS Route)

**Date**: 2026-03-19 (America/Los_Angeles)

## Purpose
Enable the installed FakeGPS Route app as the system mock?location provider so we can test altitude changes.

## Commands
```powershell
adb shell appops set com.incorporateapps.fakegps_route android:mock_location allow
adb shell settings put secure mock_location_app com.incorporateapps.fakegps_route
adb shell settings get secure mock_location_app
```

## Result (verified)
- `mock_location_app` is now set to `com.incorporateapps.fakegps_route`.

## Next Step
Open FakeGPS Route on the device and set a test location (and altitude if the app supports it). Then we can sample `dumpsys location` to verify altitude updates.
