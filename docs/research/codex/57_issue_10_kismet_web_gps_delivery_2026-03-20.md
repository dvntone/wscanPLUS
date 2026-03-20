# Issue #10 Delivery Note — Kismet Web GPS + Shared GPS Foundation

Date: 2026-03-20
Issue: `#10` Kismet remote GPS endpoint
Scope: Android-only GPS foundation and Kismet upload path

## Purpose

This note records the exact implementation direction chosen for issue `#10`, why it was chosen, what was deferred, and how it compares to the agreed plan so the next Claude/Copilot session can compare code against intent without re-deriving the design.

## Current delivery direction

### Shared Android GPS method

- Use Android fused location as the current default location source.
- Do not add a GPS-only mode in the first Kismet delivery.
- Capture and persist nullable per-scan location metadata:
  - `latitude`
  - `longitude`
  - `accuracyMeters`
  - `altitudeMeters`
  - `speedKph`
  - `locationTimestamp`
  - `locationProvider`
  - `isMockLocation`

Why:

- Android’s location guidance centers current-location retrieval around `FusedLocationProviderClient`, which is the lowest-friction high-accuracy phone/tablet path for this app’s field mode.
- Per-scan nullable GPS fields satisfy both Kismet upload correlation and the later Google Maps history feature without a second schema change.

Verified sources:

- Android location retrieval overview: [developer.android.com/develop/sensors-and-location/location/retrieve-current](https://developer.android.com/develop/sensors-and-location/location/retrieve-current)

### Kismet target surface

- First Kismet implementation target: `POST /gps/web/update.cmd`
- Deferred for later:
  - `/gps/add_gps.cmd`
  - `/gps/meta/{NAME}/update.cmd`
  - TCP NMEA output

Why:

- `web GPS` is the lowest-coupling Android-owned path.
- It works with a direct host/LAN URL and also with a USB-tunneled host-local Kismet instance.
- It does not force Android to own Kismet datasource lifecycle or remote capture naming.

Verified sources:

- Kismet GPS API: [kismetwireless.net/docs/api/gps/](https://www.kismetwireless.net/docs/api/gps/)

### Transport order

- Primary: direct Android-to-host/LAN URL
- Fallback: USB-attached host-local Kismet via `adb reverse`
- Do not treat `adb forward` as the planned upload path for Android-originated Kismet HTTP

Why:

- `adb reverse` preserves Android ownership of the HTTP client while allowing host-local Kismet to be reached as `127.0.0.1:<port>` on the device side.
- This avoids adding a desktop relay to the first `#10` PR.

## Implemented shape

At the time of this note, the implementation in the active `#10` PR adds:

- fused location sampling owned by `WatchdogService`
- in-memory latest location sample for bounded Kismet upload
- per-scan GPS persistence in Room
- Room schema migration for existing installs
- a small Kismet config surface in the Android app:
  - enabled flag
  - base URL
  - API token
- Kismet `web GPS` payload generation for:
  - `lat`
  - `lon`
  - optional `alt`
  - optional `spd`
- bounded send interval while field mode is active
- no crash / no scan interruption on Kismet upload failure

## Explicitly deferred

These are not part of the current issue `#10` implementation scope and should not be folded into this PR:

- Google Maps rendering
- history heatmap UI
- datasource-specific meta-GPS wiring
- desktop relay transport
- GPS-only operator mode
- live map overlay work
- map-provider replacement work

## Comparison to the agreed plan

| Plan item | Status | Notes |
|-----------|--------|-------|
| Fused location on Android | Implemented | Owned by `WatchdogService` via fused provider |
| Shared `LocationSample` model | Implemented | Reused for persistence + Kismet client |
| Persist per-scan nullable GPS fields | Implemented | Room migration added |
| Kismet `web GPS` first | Implemented | `/gps/web/update.cmd` path chosen |
| Direct URL first | Implemented by config model | Base URL is operator-configured |
| `adb reverse` as preferred USB fallback | Documented in settings UX and plan | Operator uses host-side `adb reverse`; Android points at `127.0.0.1:<port>` |
| Google Maps history-backed feature | Deferred | Still belongs to `#9` |
| `meta-gps` / datasource provisioning | Deferred | Not part of this PR |

## Claude comparison guidance

When comparing this delivery against Claude’s earlier plan, the main answers should be:

- **What changed now?** Android GPS capture + persistence + Kismet web GPS only.
- **What did not change yet?** Google Maps history UI, Kismet meta-GPS, desktop relay, and all map-provider alternatives.
- **Why this order?** It is the smallest Android-owned delivery that moves issue `#10` forward and also lays the storage foundation needed by issue `#9`.

## Follow-up after merge

If this `#10` PR lands cleanly, the next map work can assume:

- GPS-tagged scan rows already exist in Room
- Google Maps remains the locked Android provider
- the next feature issue is `#9`, which should build on the stored coordinates rather than reworking the location pipeline
