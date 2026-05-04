# Provider-neutral spatial map plan (MapLibre + local fallback)

Status: planning + boundary scaffolding (no map SDK dependencies)

This document captures the agreed approach for replacing the previous Google Maps-centric spatial visualization path with a provider-neutral renderer boundary.

## Goals

- Keep spatial scan/baseline visualization useful without any map SDK.
- Keep core scanning/detection provider-neutral (no map SDK types in core logic).
- Allow MapLibre as the primary optional renderer inside the Android UI layer.
- Degrade gracefully when map initialization, styles, or tiles fail.

Non-goals:

- Do not add Google Maps SDK/API.
- Do not add MapLibre in this planning PR.
- Do not mix with desktop UI work.

## Renderer boundary

Create an interface owned by the Android app UI layer (not `android/core`) so that map SDK dependencies remain optional and local to the visualization surface.

This PR includes minimal Android scaffolding for this boundary (interface + local canvas renderer) but intentionally does not add MapLibre or any other map SDK dependency.



Provider-neutral data types should remain free of any map SDK types:



## Primary renderer: MapLibre (future implementation)

- MapLibre lives behind `SpatialMapRenderer` and is created only by the spatial map screen/module.
- Map/tile style must be configurable via app config (not hard-coded in scanner/detector).
- MapLibre is a renderer only; scan/session data remains in the Room DB/state store.

## Fallback renderers

### Local static canvas fallback

If MapLibre fails, tiles are unavailable, or the device cannot initialize the renderer, the app falls back to a local static canvas that renders the best available spatial signal without any map SDK:

- relative scan points
- RSSI intensity rings (or weight markers)
- baseline zone outlines
- threat markers
- accuracy radius markers

### Non-map fallback

If map rendering cannot provide operator value (missing lat/lon, repeated renderer failure), fall back to a list/table/timeline view:

- AP observations by area/zone
- first/last seen and dwell
- RSSI trend
- threat correlation timeline
- exportable spatial observations

## Failure handling

Each failure should log a single structured event and switch to a stable fallback UI without crash loops:

- renderer init timeout
- missing/invalid style URL
- tile source unavailable
- permission denied for location
- low-memory / renderer crash
- no lat/lon available
