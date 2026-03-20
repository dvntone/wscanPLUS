# 2026-03-20 Map Provider Options

## Purpose

This note evaluates realistic alternatives or fallbacks to the currently locked Google Maps integration for the Android app. It is limited to provider fit, implementation effort, compatibility, pricing visibility, and likely drawbacks.

The repo-wide guardrail remains:

- `docs/AGENTS.md` currently locks Android mapping to Google Maps API unless maintainer direction changes.

That means this note is for future planning, not an approval to replace the existing integration.

## Current baseline: Google Maps

Why it remains the default:

- The Android repo already contains the Google Maps manifest metadata hook in `android/app/src/main/AndroidManifest.xml`.
- The project docs and guardrails already assume Google Maps on Android.
- Google provides an official Android utility library with heatmap support, which directly matches issue `#9`.

Official sources:

- Maps utility heatmaps: [developers.google.com/maps/documentation/android-sdk/utility/heatmap](https://developers.google.com/maps/documentation/android-sdk/utility/heatmap)
- March 2025 pricing update blog: [mapsplatform.google.com/resources/blog/build-more-for-free-and-access-more-discounts-online-with-google-maps-platform-updates/](https://mapsplatform.google.com/resources/blog/build-more-for-free-and-access-more-discounts-online-with-google-maps-platform-updates/)
- Current pricing landing page: [mapsplatform.google.com/intl/en_uk/pricing/](https://mapsplatform.google.com/intl/en_uk/pricing/)

Pricing note:

- Google states that as of the March 1, 2025 pricing update, free monthly usage moved from the old fixed `$200` credit to per-product free usage thresholds.
- Google’s pricing update blog also states that `Maps SDK` continues to have unlimited free usage.
- Budgeting still requires a billing-enabled Google Maps Platform project because other related products can bill separately.

Implementation fit:

- Easiest option by far because the repo is already scaffolded for it.
- Directly supports the planned Android heatmap feature via Google’s utility library.

Drawbacks:

- Vendor lock-in.
- Closed ecosystem and Google billing dependency.
- If the app later needs offline-first or self-hosted tiles, Google is the least flexible option of the candidates reviewed here.

## Candidate 1: Mapbox

Official sources:

- Android SDK guide: [docs.mapbox.com/android/maps/guides/](https://docs.mapbox.com/android/maps/guides/)
- Android install guide: [docs.mapbox.com/android/maps/guides/install/](https://docs.mapbox.com/android/maps/guides/install/)
- Pricing: [mapbox.com/pricing](https://www.mapbox.com/pricing)

What the official docs show:

- Current Android Maps SDK docs are on `v11.19.0`.
- Android requirements are compatible with this repo: Android SDK 21+, OpenGL ES 3, Java 8+, Kotlin 1.6+.
- Setup requires a Mapbox account, a public access token, and adding the Mapbox Maven repository.

Pricing note:

- Mapbox currently prices the mobile Maps SDK by monthly active users.
- Official pricing page shows:
  - up to `25,000` MAU: free
  - `25,001–125,000`: `$4.00` per `1,000`
  - `125,001–250,000`: `$3.20` per `1,000`
  - `250,001+`: `$2.40` per `1,000`

Implementation fit:

- Moderate migration effort.
- Feasible on Android because the SDK is current and officially documented.
- Good fit if the project needs more styling control than Google Maps gives.

Drawbacks:

- Requires replacing the current Google Maps integration, key handling, and likely some UI assumptions.
- Adds vendor token management and a custom repository dependency.
- Mapbox docs note attribution requirements and telemetry-related conditions.
- Starting from Maps SDK `v11.8.0`, Mapbox also brings a transitive Google Play Services dependency by default, which reduces the appeal if the goal is to move away from Google-linked dependencies entirely.

Assessment:

- Best commercial fallback if the project wants a polished hosted map stack without staying on Google.
- Not the best fallback if the primary goal is avoiding vendor/platform coupling.

## Candidate 2: MapLibre

Official sources:

- Android API docs: [maplibre.org/maplibre-native/android/api/](https://maplibre.org/maplibre-native/android/api/)
- Native repository: [github.com/maplibre/maplibre-native](https://github.com/maplibre/maplibre-native)

What the official docs show:

- MapLibre Native has a maintained Android API surface.
- The upstream repository is BSD 2-Clause licensed.

Pricing note:

- There is no vendor SDK license fee in the official MapLibre project itself.
- That does not make the full solution free in practice: tiles, styles, geocoding, routing, and hosting still need a provider or self-hosted stack.

Implementation fit:

- Highest implementation flexibility.
- Strong candidate as a fallback if the project wants to keep Android rendering independent from Google and avoid Mapbox’s commercial lock-in.
- Can pair with self-hosted or third-party tiles later.

Drawbacks:

- Highest implementation burden of the reviewed options.
- No turnkey hosted data/business layer comes with the SDK.
- A full replacement would require choosing and supporting a separate tile/style pipeline.
- Heatmaps and clustering would need to be implemented with MapLibre-compatible layers/plugins rather than the current Google utility path.

Assessment:

- Best long-term fallback if the project wants control and lower vendor lock-in.
- Worst short-term replacement if speed and low engineering overhead are the priority.

## Candidate 3: HERE SDK

Official sources:

- HERE SDK landing page: [here.com/platform/here-sdk](https://www.here.com/platform/here-sdk)
- HERE Base Plan pricing: [here.com/get-started/pricing](https://www.here.com/get-started/pricing)
- HERE commercial terms: [here.com/get-started/pricing/commercial-terms](https://www.here.com/get-started/pricing/commercial-terms)
- HERE pricing supplement: [here.com/node/53581](https://www.here.com/node/53581)
- HERE April 2025 platform release notes: [here.com/learn/blog/april-2025-platform-release-notes](https://www.here.com/learn/blog/april-2025-platform-release-notes)

What the official docs show:

- HERE positions the SDK for Android as an online/offline-capable commercial SDK.
- The pricing/search pages are partly fetch-restricted in this environment, but the official search snippets still expose current plan behavior.
- The April 2025 release notes say new customers must use the Base plan and provide a payment method to access the platform.
- The pricing pages also note a Base Plan price increase effective April 1, 2026.

Pricing note:

- HERE’s official public pricing pages are more contract-oriented than Google or Mapbox in this environment.
- Official search results confirm Base Plan onboarding now requires a payment method and that some SDK usage is billed through underlying HERE location-service transaction rates.
- That makes HERE harder to budget quickly from public docs alone than Mapbox or Google.

Implementation fit:

- Medium-to-high migration effort.
- Could fit if offline-capable commercial mapping becomes a requirement.

Drawbacks:

- Pricing and commercial terms are harder to model from public docs.
- Stronger enterprise/commercial-plan feel than the other candidates.
- The commercial-terms snippet indicates plan and feature eligibility nuances that should be reviewed carefully before using HERE as a generic drop-in replacement.

Assessment:

- Plausible enterprise alternative, but not the cleanest fallback for this repo at its current size and scope.

## Recommendation order

If the Android app must keep the current roadmap moving with the least disruption:

1. Stay on Google Maps for issue `#9`.
2. If a secondary fallback is needed, prefer Mapbox for shortest-path replacement.
3. If a true non-Google/non-Mapbox fallback is needed, evaluate MapLibre next.
4. Treat HERE as a separate enterprise-track evaluation, not the default fallback.

If the main goal changes to reducing vendor lock-in:

1. Evaluate MapLibre first.
2. Keep Google as the current production default until a tile/style strategy exists.
3. Treat Mapbox as a commercial convenience option, not the lock-in-minimizing path.

## Compatibility notes for wscan+

- `minSdk 24` is compatible with the reviewed Android SDK candidates.
- The current repo already stores a `GOOGLE_MAPS_API_KEY` path and expects Google Maps metadata in the manifest, so any replacement is not a trivial drop-in.
- The Android feature on the roadmap is specifically a heatmap and scan-history visualization. Google already has a first-party utility path for this; Mapbox and MapLibre would require a different implementation shape.
- Because the repo guardrails explicitly preserve Google Maps in the Android app, any actual provider swap should start as a tracked issue and architecture decision, not an opportunistic code change.
