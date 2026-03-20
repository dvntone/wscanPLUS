# Altitude/Height Availability (ADB Location)

**Date**: 2026-03-19 (America/Los_Angeles)

## Purpose
Check whether altitude/height data is available from Android location providers to support floor?level inference.

## Commands
```powershell
adb shell dumpsys location
adb shell dumpsys location | Select-String -Pattern 'last location|alt=' -Context 0,2
```

## Observed (from dumpsys location)
- Network provider **last location** included altitude:
  - `alt=174.8000030517578` with `vAcc=1.6336432`
- Fused provider **last location** included altitude:
  - `alt=174.8000030517578` with `vAcc=1.0757003`
- GPS provider **last location** was `null` and `mStarted=false` at time of check.

## Interpretation (verified)
- Altitude data **is available** (from network/fused provider) in dumpsys output on this device.
- Vertical accuracy (`vAcc`) is present; this can be used to gate whether altitude is reliable enough for floor estimation.

## Notes for Integration
- This altitude value comes from **network/fused** provider, not GPS.
- Use `vAcc` as a confidence metric before converting altitude to floor.

## Privacy
Altitude logs include precise location coordinates in `dumpsys location` output. Treat as sensitive.
