# 2026-04-25 Free-Only Map Migration Track

## Purpose

This note records a no-new-paid-services path for replacing the current Google Maps heatmap later, after the active Google Play smoke test work is complete.

It does not change Android code. The current worktree already has active Android and desktop changes, and Claude is smoke-testing the Google Play path. Any map-provider replacement should be its own tracked issue and PR because it changes dependencies, app behavior, secrets, threat-model assumptions, and release validation.

## Current Billing Finding

Google Maps Platform can still produce a recurring-looking bill on pay-as-you-go if a billable SKU is being hit often enough. Dynamic Maps currently has a 10,000 monthly free usage cap and then bills per 1,000 map loads. That means a relatively small number of web map loads can reach about 100 USD/month.

The Android native Maps SDK itself is listed by Google as unlimited free in the core pricing list. If the invoice SKU is `Maps SDK`, that should be treated as a billing dispute candidate. If the invoice SKU is `Dynamic Maps`, `Map Tiles API`, `Geocoding`, `Places`, `Routes`, or `Street View`, the charge is coming from a billable API/SKU outside the native Android SDK free line.

## Free-Only Decision

Use this priority order for future work:

1. MapLibre renderer in the Android app.
2. OpenFreeMap public styles/tiles for development and proof-of-concept only.
3. PMTiles/Protomaps self-hosted or bundled regional tiles for a production no-recurring-provider path.
4. No Mapbox, MapTiler paid tier, Stadia paid tier, HERE, or other paid account unless the maintainer explicitly reopens the cost decision.

This is not "zero engineering cost." It is "no new recurring map-provider bill."

## Why MapLibre

MapLibre is the best fit for a free-only migration because:

- The SDK is open source and not tied to Google Maps Platform billing.
- It supports native heatmap-style layers, including radius, weight, intensity, color, and opacity.
- wscan+ already stores GPS-tagged scan rows and threat confidence values, which can be converted into a GeoJSON source for a weighted heatmap layer.
- It keeps the app flexible enough to use OpenFreeMap, local/self-hosted PMTiles, or another tile source later without rewriting the scan-data overlay logic again.

## Why Not Direct OSM Tiles

Do not point production app traffic at the OpenStreetMap Foundation standard tile servers. The OSM tile policy is not a free CDN for mobile-app usage. Use a provider that explicitly allows the use case, or self-host/cache tiles.

## Recommended Implementation Shape

Future Android PR:

- Replace the `SupportMapFragment` Google implementation in `ScanMapActivity` with a MapLibre map view.
- Convert `scanResultDao().getGpsTagged(limit = 500)` rows into GeoJSON point features.
- Add a `threatWeight` property per feature from the current `threatSignalDao().getHighConfidence(...)` confidence grouping.
- Render a MapLibre heatmap layer using `heatmap-weight` from `threatWeight`.
- Render a circle layer at high zoom so individual scan points remain inspectable.
- Keep "center on me" using the existing Android location path; do not replace location sampling just to migrate maps.
- Keep Gemini/Vertex untouched.

Dependency PR boundary:

- Add MapLibre as a dependency in a dedicated dependency-change PR.
- Remove `play-services-maps`, `android-maps-utils`, `GOOGLE_MAPS_API_KEY`, and Google map manifest metadata only in the same dedicated map-provider replacement PR, not in unrelated feature work.
- Update `docs/DEPENDENCIES.md`, `docs/SECRETS.md`, `docs/THREAT_MODEL.md`, `docs/PROJECT_OVERVIEW.md`, `docs/ROADMAP.md`, and `docs/SESSION_STATE.md` in the same PR if the provider actually changes.

## No-Cost Tile Options

### OpenFreeMap Public Instance

Use for a first proof-of-concept because it requires no API key and no account.

Risks:

- No project-controlled SLA.
- External availability is donation/community-dependent.
- Production reliance should be a maintainer decision, not an implicit default.

### PMTiles / Protomaps

Use for the production free-only path if avoiding recurring provider bills is the priority.

Likely shape:

- Build or download a regional PMTiles/MBTiles dataset for expected operating areas.
- Host it from a maintainer-controlled static endpoint or bundle/cache region packs where size allows.
- Use MapLibre with a PMTiles-compatible source/protocol where supported by the Android stack, or convert to an Android-friendly local tile source.

Risks:

- More engineering and release packaging work than hosted tiles.
- Requires an update process for map data.
- Global offline tiles are too large for ordinary app distribution; region packs are more realistic.

## Billing Containment Checklist

Do this in Google Cloud before or during the migration:

- In Billing Reports, group Google Maps Platform costs by SKU.
- Identify the exact SKU causing the charge.
- In Google Maps Platform Metrics, group usage by project, credential, and API.
- Disable unused APIs: Maps JavaScript API, Geocoding, Places, Routes, Map Tiles API, Street View, and Static Maps unless a tracked feature needs them.
- Restrict Android keys by package name and SHA certificate fingerprint.
- Restrict web keys by HTTP referrer or disable them.
- Set daily quotas to zero or near-zero for every unused billable SKU.
- Add a budget alert below the pain threshold, not at the old invoice amount.

## Proposed Issue

Title:

```text
Replace Google Maps heatmap with free-only MapLibre path
```

Body:

```text
## Goal

Remove wscan+'s dependency on Google Maps Platform for the Android scan heatmap so the map feature cannot generate recurring Google Maps charges.

## Scope

- Replace Google Maps rendering in ScanMapActivity with MapLibre.
- Render GPS-tagged scan rows as a weighted heatmap layer.
- Use threat confidence as heatmap weight.
- Keep center-on-current-location behavior.
- Keep Gemini/Vertex integration untouched.
- Use OpenFreeMap only for proof-of-concept or use self-hosted/local tiles for production.

## Non-goals

- No paid Mapbox/MapTiler/Stadia/HERE account.
- No changes to Gemini/Vertex.
- No routing, geocoding, Places, or Street View.
- No direct production use of OSMF standard tiles.

## Acceptance Criteria

- Android app builds without GOOGLE_MAPS_API_KEY.
- Google Maps SDK and android-maps-utils are removed.
- Scan map displays stored GPS-tagged scan points.
- Heatmap weights reflect existing threat confidence.
- Empty/no-location states still display useful UI.
- Required Android checks pass.
- Docs and dependency records are updated.
```

## Current Recommendation

Do not touch Android map code while Google Play smoke testing is active.

After the smoke test is complete, create the issue above and implement the migration as one focused dependency-and-map-provider PR. Until then, use Google Cloud quotas/API restrictions to stop billable SKU traffic.
