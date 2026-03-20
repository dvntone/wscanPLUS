# Mock Location Altitude Capture (FakeGPS Route)

**Date**: 2026-03-19 (America/Los_Angeles)

## Purpose
Verify that the FakeGPS Route mock?location app can inject altitude and that Android reports it as a mock location.

## Commands
```powershell
adb shell dumpsys location | Select-String -Pattern 'last location=Location\[fused|last location=Location\[network|last location=Location\[gps' -Context 0,1
```

## Observed (from dumpsys location)
- Fused provider reported:
  - `alt=103.0` and `mock` flag present.
- Network provider reported:
  - `alt=103.0` and `mock` flag present.
- GPS provider reported:
  - `alt=103.0` and `mock` flag present.

Excerpt indicators:
- `last location=Location[fused ... alt=103.0 ... mock]`
- `last location=Location[network ... alt=103.0 ... mock]`
- `last location=Location[gps ... alt=103.0 ... mock]`

## Sampling (fused)
We sampled the fused altitude every ~6 seconds:

| Time | Altitude | Mock | 
| --- | --- | --- |
| 08:19:22 | 103.0 | True |
| 08:19:28 | 103.0 | True |
| 08:19:34 | 103.0 | True |
| 08:19:41 | 103.0 | True |

## Interpretation (verified)
- The mock app successfully injects altitude into fused/network/gps providers.
- The OS marks these locations as `mock`, which the app can use to guard against false floor inference in production.

## Integration Notes
- For real deployments, mock?flagged altitude should be ignored or explicitly flagged.
- For testing, this provides a reliable way to validate altitude and floor?level logic.
