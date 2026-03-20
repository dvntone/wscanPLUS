# Altitude Stability Sampling (Live Check)

**Date**: 2026-03-19 (America/Los_Angeles)

## Purpose
Sample altitude values over time to see whether the altitude is updating (useful for floor?level inference).

## Commands
```powershell
adb shell dumpsys location | Select-String -Pattern "last location=Location\[fused"
```

A scripted sample extracted `alt`, `vAcc`, `hAcc`, and `et` every ~8 seconds.

## Samples (fused provider)
| Time | Altitude | vAcc | hAcc | et |
| --- | --- | --- | --- | --- |
| 08:12:08 | 174.8000030517578 | 1.759891 | 13.591 | +1d17h0m59s961ms |
| 08:12:16 | 174.8000030517578 | 1.759891 | 13.591 | +1d17h0m59s961ms |
| 08:12:24 | 174.8000030517578 | 1.759891 | 13.591 | +1d17h0m59s961ms |
| 08:12:32 | 174.8000030517578 | 1.759891 | 13.591 | +1d17h0m59s961ms |

## Interpretation (verified)
- The altitude and accuracy values **did not change** across ~24 seconds.
- The `et` (elapsed?time) value was constant, indicating the fused last location **did not refresh** during this sampling window.

## Integration Notes
- For floor?level inference, the app must request active location updates; relying on stale `dumpsys` last?location values will not provide real?time floor changes.
- Use `vAcc` to decide whether altitude is reliable enough for floor estimation.

## Privacy
The raw `dumpsys location` output includes precise coordinates. This doc intentionally omits coordinates.
