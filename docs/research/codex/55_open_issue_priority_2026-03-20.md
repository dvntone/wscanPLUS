# 2026-03-20 Open Issue Priority

## Purpose

This note ranks the remaining open `wscanplus` issues after the March 20, 2026 triage fixes and links each recommendation to verified sources.

Current open issues at time of writing:

- `#121` docs/session cleanup follow-up
- `#122` missing visible app-side adb logs on Revvl Android 15
- `#124` coarse-only behavior now blocks before service startup on current `main`
- `#125` backgrounded / keyguard-visible app loses effective `getScanResults()` access
- `#9` Google Maps threat heatmap + scan history map
- `#10` Kismet remote GPS endpoint

Primary sources used:

- Android Wi-Fi scanning overview: [developer.android.com/develop/connectivity/wifi/wifi-scan](https://developer.android.com/develop/connectivity/wifi/wifi-scan)
- Request background location: [developer.android.com/develop/sensors-and-location/location/permissions/background](https://developer.android.com/develop/sensors-and-location/location/permissions/background)
- Foreground service restrictions and while-in-use permissions: [developer.android.com/develop/background-work/services/fgs/restrictions-bg-start](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)
- Android logcat tool: [developer.android.com/tools/logcat](https://developer.android.com/tools/logcat)
- Google Maps Android Utility heatmaps: [developers.google.com/maps/documentation/android-sdk/utility/heatmap](https://developers.google.com/maps/documentation/android-sdk/utility/heatmap)
- Google Maps Android Utility library overview: [developers.google.com/maps/documentation/android-sdk/utility](https://developers.google.com/maps/documentation/android-sdk/utility)
- Kismet GPS API: [kismetwireless.net/docs/api/gps/](https://www.kismetwireless.net/docs/api/gps/)
- Kismet datasource / meta-GPS docs: [kismetwireless.net/docs/readme/datasources/datasources/](https://www.kismetwireless.net/docs/readme/datasources/datasources/)

## Priority order

### 1. `#125` Background/keyguard scan access on Android 15

Why first:

- This is the highest-impact remaining runtime issue because it affects the app’s stated long-running field use case.
- Android’s official guidance says a foreground service with a `location` type plus `ACCESS_BACKGROUND_LOCATION` can access location information all the time, even when the app runs in the background, in the defined permitted situations. The current repo should align behavior and operator flow to that model before taking on new features.
- Current main already declares `ACCESS_BACKGROUND_LOCATION`, uses `foregroundServiceType="location|dataSync"`, and gates startup on the background-location settings path. The Revvl path has not yet been re-tested after those changes, so the next step is a re-verification pass against current main rather than another blind permission patch.
- Live Revvl re-test update from this session: the non-secure-lockscreen path on current `main` did not reproduce the old failure. Foreground launch worked, `WatchdogService` stayed foreground after `HOME`, and it also stayed foreground after screen-off. That narrows the remaining question to secure-lockscreen and other stronger background cases rather than generic non-secure backgrounding.

Verified source notes:

- Android documents that location access from the background is special-case behavior and should be critical to core functionality.
- Android also documents that background access and while-in-use restrictions behave differently on modern versions, especially with foreground services and location-typed services.

Recommended fix direction:

- Re-validate current `main` on the Revvl secure-lockscreen path first, because the non-secure HOME / screen-off pass no longer reproduces the old denial.
- Verify whether the failure is still present after the service starts as a `location|dataSync` foreground service on current main under the stricter lock-state conditions.
- If the failure persists, add explicit service-side state reporting for “field mode verified” versus “foreground-only / recovery needed” rather than assuming the initial activity-side permission gate is enough.
- Keep this work Android-only and avoid mixing in mapping or Kismet transport.

Compatibility note:

- Must preserve the current Android 14 behavior that already recovered on the moto g play path.
- Must not regress the existing API 24–29 scanner compatibility fix.

### 2. `#124` Coarse-only degraded path

Why second:

- The repo already documents that coarse-only is degraded-only, not fully scan-capable.
- Android’s Wi-Fi scanning docs say Android 10+ `startScan()` requirements differ by target SDK and permission state, while the background-location docs say approximate choice also affects background accuracy.
- Current main already reflects that stance in code by stopping before scanner startup when precise location is missing. The Revvl has now been re-tested on current `main`, so this issue is primarily docs/UX alignment unless another device still reaches an active scanner state without fine location.
- Product-wise, a normal coarse-only scanner mode would cut against the app's main purpose. At most, it belongs in a later optional degraded-function path with clear user notice.
- Live Revvl re-test update from this session: with `ACCESS_FINE_LOCATION` absent and `COARSE` + `BACKGROUND` + `NEARBY` granted, current `main` launched `MainActivity` but did not start `WatchdogService`. That means the old “service starts then scan retrieval fails” symptom is no longer the current-main behavior.

Verified source notes:

- Android Wi-Fi scan documentation distinguishes permission requirements by OS/target SDK.
- Android background-location docs explicitly state that approximate foreground access also implies approximate background access.

Recommended fix direction:

- Treat this first as issue/docs alignment: current `main` already blocks coarse-only before service startup on the Revvl re-test.
- If any device still lets coarse-only reach an active scanner state, capture that exact path and patch only that gap.
- Keep the current fine-location gate unless product direction explicitly chooses to add an optional degraded mode later.
- Do not attempt to “paper over” this with hidden retry logic.

Compatibility note:

- Keep a degraded path only if the app can state exactly what still works under coarse-only.
- If not, prefer explicit escalation over ambiguous partial behavior.

### 3. `#122` Revvl Android 15 app-side adb log visibility

Why third:

- This is important for diagnostics and CI-style field verification, but it is not the primary product behavior.
- Android’s logcat docs note that multiple buffers exist and not all messages are visible in the default view, so the first step should be verification of the logging path, buffers, tag filtering, and priority levels before changing app code.
- Current code already logs from `MainActivity`, `WatchdogService`, and `StandardScanner`, so the issue still looks like an observability/runtime problem first.
- Live Revvl re-test update from this session: app debug tags were visible on current `main` once the device was truly unlocked, the app was foregrounded, and tag priority was raised for capture. The remaining gap is a repeatable operator procedure for this OEM path, not proof that ADB cannot see app logs.

Verified source notes:

- Android documents `main`, `system`, `crash`, and `all` log buffers.
- The `Log` API writes to logcat, but visibility and filtering depend on the buffer selection and runtime environment.

Recommended fix direction:

- Treat this as an observability task first, not a functional runtime bug.
- Verify whether the missing logs are:
  - tag/filter usage,
  - buffer selection,
  - OEM/runtime suppression,
  - or app logging level/placement.
- If app changes are needed, prefer a small structured logging pass for the key scanner lifecycle transitions only.

Compatibility note:

- Avoid broad log spam that could make the app noisier or expose internal detail unnecessarily.
- Keep any new logging bounded to the tags already used by the scanner stack.

### 4. `#10` Kismet remote GPS endpoint

Why fourth:

- This is the highest-value remaining feature item because it unlocks a concrete Android-to-desktop integration path without requiring the full map feature to land first.
- Kismet’s GPS API and meta-GPS docs already provide a defined target surface.

Verified source notes:

- Kismet documents adding GPS devices via `/gps/add_gps.cmd`.
- Kismet also documents web GPS updates via `/gps/web/update.cmd`.
- Kismet datasource docs explicitly describe attaching a “meta-gps” to a datasource for remote capture.

Recommended fix direction:

- Keep the first implementation minimal:
  - one outbound GPS transport path
  - clear payload mapping from Android location data
  - no combined map/history UI in the same PR
- Chosen direction for the first delivery:
  - Android fused location as the shared location source
  - nullable per-scan GPS persistence in Room
  - Kismet `web GPS` via `/gps/web/update.cmd`
  - operator-configured base URL + token
  - direct host/LAN URL first, `adb reverse` as the preferred USB fallback
- Defer `/gps/add_gps.cmd`, datasource-linked `meta-gps`, and TCP NMEA output to later work.

Compatibility note:

- Prefer an HTTP/REST path that works with the existing desktop/host assumptions and does not require Android-side native Kismet dependencies.
- Keep Kismet delivery Android-only. Do not mix in Google Maps UI or alternate map-provider work in the same PR.

### 5. `#9` Google Maps threat heatmap + scan history map

Why fifth:

- Official Maps support is strong, and the utility library directly supports heatmaps, but this is still downstream of getting the scan/runtime behavior stable.
- It is feature work, not a blocker for the scanner itself.

Verified source notes:

- Google’s Maps Android Utility Library includes a heatmap utility.
- The utility library also supports markers, clustering, and dynamic overlay updates.

Recommended fix direction:

- Implement this as a presentation layer on top of already-stored GPS-tagged scan data.
- Use marker/clustering for discrete events and heatmaps for dense distributions, rather than trying to force one visualization for both.
- Assume issue `#10` provides the shared location/persistence foundation first; do not reopen the GPS storage design while implementing the map.

Compatibility note:

- Preserve the current paid-for Google Maps integration boundary documented in the repo guardrails.
- Do not mix the map feature with scanner-permission/runtime fixes.

### 6. `#121` docs/session cleanup

Why last:

- Important for cleanliness, but lower urgency than runtime issues and the two planned feature integrations.

Recommended fix direction:

- Fold this into the next docs-only maintenance pass after the runtime issues settle.

## Suggested execution sequence

1. `#125`
2. `#124`
3. `#122`
4. `#10`
5. `#9`
6. `#121`

## Notes for future agents

- `#143` and `#145` are resolved and closed.
- Desktop issue `wscanplus_desktop#21` is resolved and closed.
- The review timing baseline from this session matters: Copilot comments may arrive after the first 3 minutes, and an extra ~90 second follow-up check was still productive.
- Map-provider replacement or fallback research is recorded in `56_map_provider_options_2026-03-20.md`; do not propose a Google Maps swap without reading that note and the repo guardrails first.
