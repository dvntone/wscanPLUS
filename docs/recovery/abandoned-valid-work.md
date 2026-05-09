# Abandoned / Partial Work Recovery Ledger

## Purpose

This document tracks abandoned, partial, superseded, or unsafe branches/PRs that still contain potentially valid implementation ideas.

Rules:
- Do not merge abandoned branches directly.
- Recreate valid concepts on top of current `main`.
- Preserve deterministic/explainable architecture.
- Avoid ML, SDR assumptions, attribution claims, or fake alerts.
- Keep recovery PRs isolated and reviewable.

## Classification Key

| Status | Meaning |
|---|---|
| SAFE_RECREATE | Concept is valid and can be rebuilt safely in isolated PRs |
| REDESIGN_REQUIRED | Concept useful but implementation shape conflicts with current architecture |
| SUPERSEDED | Already replaced or absorbed elsewhere |
| DO_NOT_REVIVE | Conflicts with architecture or project constraints |

## PR #277 — Room-backed ScanContext population

Status: REDESIGN_REQUIRED

### Valid concepts
- Persistent known-network profiles.
- Baseline-aware contextual detector enrichment.
- Historical scan context reuse.
- Future evidence aggregation support.

### Problems found
- Baseline calculations mixed incompatible scopes.
- Per-session distinct counts compared against single scan snapshots.
- Open/incomplete sessions could contaminate baseline calculations.
- Risk of synchronous DB pressure during scans.

### Recovery approach
1. Add completed-session baseline queries only.
2. Separate batch-level and session-level statistics.
3. Add DAO-level tests before detector integration.
4. Integrate knownProfiles first.
5. Delay statistical drift heuristics until validated.

## PR #291 — Local heatmap + provider-neutral spatial work

Status: SAFE_RECREATE

### Valid concepts
- Provider-neutral local renderer.
- Offline-capable heatmap.
- Local-only spatial visualization.
- Future explainable evidence overlays.

### Incorporated work
- LocalHeatmapView cache optimization.

### Must not copy
- Google Maps dependency.
- Provider-specific map SDKs.
- Network-backed tile assumptions.

## PR #292 — NMEA transport / BLE tethering

Status: REDESIGN_REQUIRED

### Valid concepts
- Optional desktop companion ingestion.
- Local-only telemetry forwarding.
- Deterministic GPS/NMEA formatting.
- USB/BLE companion transport abstraction.

### Recovery approach
1. Define ingestion boundary interfaces.
2. Add explicit user opt-in.
3. Keep transport separate from detector logic.
4. Normalize imported observations before aggregation.

## PR #267 — AP inventory controls

Status: SAFE_RECREATE

### Incorporated work
- Observer suppression guards.
- Stable render lifecycle.
- Safer numeric sorting.

## PR #270 — Desktop BSSID inspector

Status: REDESIGN_REQUIRED

### Valid concepts
- Facts vs inference separation.
- Explainable detector presentation.
- Confidence + rationale display.

### Incorporated work
- Observer suppression guards.
- Stable inspector render lifecycle.

### Recovery approach
1. Replace DOM-derived parsing with normalized evidence adapters.
2. Add provenance/source rendering.
3. Separate observed facts from detector interpretation.

## Branch — claude/update-profile-reply-status-zgvs2

Status: REDESIGN_REQUIRED

### Contains
- Cell observation collector.
- Rayhunter transport.
- ntfy publisher.
- Desktop preload/store changes.
- Android notification integration.

### Recovery approach
Split into isolated PRs:
1. CellObservation model only.
2. Passive collector only.
3. Desktop ingestion abstraction.
4. Optional ntfy notifier.
5. Rayhunter transport adapter.

### Must not copy
- Direct detector attribution.
- "Stingray detection" wording.
- Automatic network publication.
- Monolithic merge strategy.

## Explicitly rejected recovery paths

Status: DO_NOT_REVIVE

- Google Maps dependency restoration.
- Provider-locked spatial rendering.
- SDR assumptions in Android core.
- Monitor-mode assumptions on stock Android.
- ML-based detector claims.
- Fake/demo alerts in production paths.
- Attribution/identity claims.
- "Detects Stingrays" wording.
- Runtime permission bypasses.
- CI suppression hacks.
- Direct UI-text parsing as detector truth.
