# Pixel 10 Pro XL

**Updated**: 2026-03-20  
**Track**: latest beta track reported by operator  
**ADB-reported platform values**:

- `ro.build.version.release=16`
- `ro.build.version.sdk=36`
- `ro.build.version.codename=CinnamonBun`

## Purpose

Track the strictest modern-device validation target for wscan+.

This device is useful because it combines:

- latest beta-track Pixel software
- Advanced Protection enabled
- built-in Linux terminal availability

## Current baseline

- Developer options enabled
- Wi-Fi scan throttling disabled
- device-wide location mode observed as `0` via `adb shell settings get secure location_mode` (`0` = location off; `1` = device only; `2` = Wi-Fi/cell; `3` = high accuracy) at first connection and must be explicitly re-checked before drawing scan conclusions

## Current adb / Advanced Protection baseline

Observed on 2026-03-20:

- `adb install -r` of the current debug app succeeded from an already trusted host while Advanced Protection remained enabled
- the same trusted adb session retained shell access with the lockscreen showing
- a repeat `adb install -r` also succeeded with the lockscreen showing

Interpretation:

- For the current trusted-host setup, Advanced Protection does not appear to require disabling before `adb install -r` (install/update) of the test app
- future testing should still distinguish:
  - trusted already-authorized host vs new host
  - unlocked vs locked device state
  - shell access vs install/update behavior

## Next validation

- Full matrix result is now documented in `2026-03-20-full-adb-matrix.md`.
- Current conclusions:
  - full-permission foreground, true background, secure keyguard, and post-unlock recovery all pass on the current Pixel setup
  - coarse-only still behaves as degraded/incomplete rather than fully scan-capable
  - `location_mode` must still be checked before drawing scan conclusions
- Post-session state after the full matrix:
  - device location returned to off
  - test app removed from the device
