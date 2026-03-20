# Android Wi-Fi Location Strategy

**Updated**: 2026-03-20  
**Scope**: Android 15 / targetSdk 36 behavior for wscan+

## Purpose

Record the current platform facts behind wscan+'s Wi-Fi scanning permission model so future sessions do not reopen the same questions about:

- `ACCESS_FINE_LOCATION` vs `ACCESS_COARSE_LOCATION`
- whether `NEARBY_WIFI_DEVICES` replaces location for scan results
- whether `ACCESS_BACKGROUND_LOCATION` ("Allow all the time") is warranted
- whether unrelated permissions such as phone state are needed

## Current platform constraints

For apps targeting Android 10+:

- `WifiManager.getScanResults()` requires:
  - `ACCESS_FINE_LOCATION`
  - `ACCESS_WIFI_STATE`
  - device location services enabled
- `ACCESS_COARSE_LOCATION` alone is not sufficient for the current scan-results path.
- `NEARBY_WIFI_DEVICES` does not replace `ACCESS_FINE_LOCATION` for `getScanResults()`.

This matches the current Revvl Tab 2 and Pixel evidence:

- coarse-only permission allows the app to launch and start the service
- actual scan retrieval still fails on the current path
- full fine location restores the expected Wi-Fi service interaction
- once the UI is backgrounded or sitting behind keyguard, `WatchdogService` can remain foreground while `WifiService` still denies `getScanResults()` for the app UID

## What this means for wscan+

### 1. Coarse-only should be treated as degraded UI permission, not full scan capability

The app can still use coarse-only as a better-than-deny state for onboarding and controlled fallback behavior, but it should not imply that reliable Wi-Fi scan retrieval is available on modern Android.

Practical interpretation:

- coarse-only is useful to avoid a dead-end permission flow
- coarse-only is not enough to promise normal scanner behavior on targetSdk 36
- app UX should treat it as a fallback / limited mode, not the default or recommended operating mode
- because wscan+ is primarily a scan-driven defensive tool, coarse-only should not become the main product path
- if retained at all in future builds, coarse-only should be an explicitly optional degraded function with operator-facing notice about reduced capability

### 2. `ACCESS_BACKGROUND_LOCATION` is part of the current long-running collection model

Conceptually, a purely foreground/manual test flow could operate without background location,
but on Android 10+ the intended wscan+ field mode depends on background continuity, so
`ACCESS_BACKGROUND_LOCATION` should be treated as a required permission for the primary
long-running detection/logging path.

However, wscan+ is not a casual consumer app. Its role is long-running defensive detection, logging, and field collection. If the intended product behavior is:

- continuing scan-driven detection after the activity is no longer visible
- maintaining reliable logging and evidence capture during prolonged field sessions
- supporting foreground-service-driven operation as the primary operator model

then `ACCESS_BACKGROUND_LOCATION` should be treated as part of the implementation, not a speculative option.

That is a technical capability question first, not a Play policy question.

Pre-fix empirical signal on Revvl Tab 2 / Android 15:

- visible `MainActivity` + fine location -> scan-result access works
- HOME / screen-off / keyguard-visible state -> service survives, but app scan-result access is denied as lacking location permission

That makes background-location evaluation materially relevant for this app's intended discreet/background operation model.

Cross-device confirmation as of 2026-03-20:

- moto g play - 2024 / Android 14 reproduces the same failure under a real secure keyguard state
- shell-level Wi-Fi scanning continues on both devices while app UID scan access fails

This means the behavior was not a single OEM anomaly.

Current code-and-test state on 2026-03-20:

- the manifest declares `ACCESS_BACKGROUND_LOCATION`
- `WatchdogService` runs as `foregroundServiceType="location|dataSync"`
- `MainActivity` now blocks scanner startup without precise location and prompts for background location before starting field mode
- moto g play - 2024 / Android 14 showed an improved locked-screen result in one validation pass once device location mode was enabled
- Revvl Tab 2 / Android 15 has now been re-tested on current `main` for the non-secure foreground/background path, so issues `#124` and `#125` should now be read as narrowed follow-ups rather than untouched pre-fix validation
- Pixel 10 Pro XL has its own documented matrix, but that result set should not be generalized onto the Revvl path

The current evidence supports keeping background location in scope for wscan+'s discreet / long-running field mode, but not treating it as fully solved across all Android 15 / OEM paths.

### 3. Background location is still not free

Even for a private/FOSS release, Android platform limits still apply:

- background location should be justified by core functionality
- Android limits background location update frequency to preserve battery
- long-running collection increases battery cost and operator-visible privacy surface

So the decision is not "avoid it because of Play Store", but:

- add it only if it materially improves real scanner reliability when the UI is not foregrounded
- pair it with explicit operator disclosure and a clear mode model
- test battery impact on real devices before making it default

### 4. Phone permission is not indicated by current evidence

There is no current evidence that phone-state permission is required for:

- Wi-Fi scan result access
- scanner chain selection
- WatchdogService lifecycle
- current Revvl Tab 2 debugging

Do not add phone-related permissions unless a later feature explicitly depends on telephony APIs.

## Recommended product stance

Near-term stance for wscan+:

1. Keep foreground fine-location behavior as the known-good baseline.
2. Keep coarse-only support documented as degraded and incomplete for scan retrieval.
3. Treat `ACCESS_BACKGROUND_LOCATION` as required for the explicit field logging / long-running detection mode.
4. Treat any future coarse-only path as optional degraded mode only, with explicit notice that it is not the core scanner mode.
5. Do not add phone permissions for scanner debugging.
6. Separate already-trusted-host adb behavior from first-trust onboarding claims on strict-security Pixel devices.

## Next validation work

Now that `ACCESS_BACKGROUND_LOCATION` is implemented, validate the remaining edge cases on current hardware:

1. Measure practical battery cost during a fixed interval capture run on at least one Android 14+ phone and the Revvl tablet.
2. Decide whether the app should hard-require "Allow all the time" or expose a narrower foreground-only mode.
3. Decide whether coarse-only should remain an allowed degraded state or redirect operators into an explicit fine-location upgrade path.
4. Validate first-trust host behavior separately from already-trusted-host behavior on the Pixel / Advanced Protection path.

## Sources

- [Wi-Fi scanning overview](https://developer.android.com/develop/connectivity/wifi/wifi-scan)
- [Request location access at runtime](https://developer.android.com/develop/sensors-and-location/location/permissions/runtime)
- [Access location in the background](https://developer.android.com/develop/sensors-and-location/location/background)
