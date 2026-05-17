# Threat Actor Persistence Layer — Feature Concept

**Date:** 2026-05-07  
**Phase target:** Phase 5 / Phase 6  
**Motivation:** HawkEye 360 "Vessel Custody ID" + Anduril Lattice entity aliases + WhoFi RF fingerprinting research  
**Status:** Concept only. No implementation. Requires threat signal accumulation work to be stable first.

---

## Problem

Every threat detection in wscanplus is currently **siloed per scan session**. When a session ends and a new one begins, the system has no memory of previous attacker devices. A motivated attacker who simply rotates their MAC address between sessions becomes invisible to all historical detection — including `BssidFingerprintHeuristic`, which only fires within a session.

This means a single session's evidence is inherently thin. One rogue AP event, one session of SSID flooding — these are dismissible as coincidence or equipment malfunction. What would be compelling to building management or law enforcement is a **30-day log showing the same device appearing 47 times**, persisting through 12 different BSSIDs.

Currently wscanplus cannot produce that report because it has no way to assert "these 12 different BSSIDs are the same physical device."

---

## Concept

Introduce a `ThreatActorEntity` — a persistent record representing a suspected attacker device, identified by a UUID that survives BSSID rotation. This entity accumulates evidence across sessions, linking multiple `ThreatSignal` records to a single threat actor over time.

### What Makes a ThreatActorEntity

The identity is derived from RF features that do not change when a MAC address is rotated:

| Feature | Rationale |
|---------|-----------|
| Channel preference | Attacker hardware tends to operate on the same channel(s) between sessions |
| Beacon timing pattern | Sub-ms beacon interval jitter is hardware-specific |
| OUI family (first 3 bytes) | Even with randomized MACs, some chipsets use OUI ranges that narrow the device class |
| RSSI spatial signature | Same physical hardware in the same location produces characteristic RSSI across multiple scan vantage points |
| Capability flags | 802.11 capability bits (HT, VHT, supported rates) are hardware-determined and rarely change |
| Security configuration pattern | Attacker tools tend to use the same cipher suite configurations across rotations |

No single feature is uniquely identifying. The combination, particularly when a new BSSID appears with the same channel + capability profile + spatial RSSI signature as a previously-flagged device, is a strong signal.

### Data Model Sketch

```kotlin
// New Room entity — one row per suspected attacker device
@Entity(tableName = "threat_actors")
data class ThreatActorEntity(
    @PrimaryKey val actorId: String,          // UUID, stable across sessions
    val firstSeenMs: Long,
    val lastSeenMs: Long,
    val signalCount: Int,                     // total ThreatSignal records linked
    val sessionCount: Int,                    // distinct sessions where actor appeared
    val observedBssids: String,               // JSON array of BSSIDs attributed to this actor
    val channelPattern: String,               // JSON array of channels most frequently used
    val peakConfidence: Float,                // highest confidence across all sessions
    val cumulativeConfidence: Float,          // accumulated confidence (decayed over time)
    val lastHeuristicTags: String,            // JSON array of most recent heuristic names
    val notes: String,                        // human-readable summary for export
)
```

### Linking Logic

When a new `ThreatSignal` is generated, the system attempts to attribute it to an existing `ThreatActorEntity` before creating a new one:

1. Exact BSSID match in `observedBssids` → direct link
2. Same channel + capability flags + RSSI within tolerance of a known actor → candidate match (confidence-gated)
3. No match → create new `ThreatActorEntity`

The attribution confidence must exceed a threshold before linking. If confidence is below threshold, the signal remains unattributed and may be linked retroactively when more evidence accumulates.

### Decay Model

`cumulativeConfidence` decays over time to prevent stale threat actors from permanently inflating scores. A simple half-life model: confidence halves every 7 days of inactivity. An actor that has not been observed in 30 days is archived (retained for export but excluded from active scoring).

---

## Prerequisites

Before implementing:
1. `knownProfiles` must be wired into `ScanContext` (current Phase 2 drift item) — provides the historical BSSID data needed for cross-session BSSID matching
2. Per-session `ThreatSignal` accumulation must be stable (currently complete via Room DB)
3. Multi-device support in the data model (Phase 5) — actor sightings should aggregate across Android devices

---

## Evidence Output

The primary value is in reporting. A `ThreatActorEntity` with `sessionCount = 23`, `signalCount = 147`, `firstSeenMs` 30 days ago, and `observedBssids` containing 8 different MAC addresses is a materially different evidence artifact than any single scan event.

Export format: JSON (current export mechanism) + human-readable summary block for the Tier 1 user narrative.

---

## Phase Placement

**Phase 5 (partial):** Add `ThreatActorEntity` Room entity and DAO. Wire linking logic into `WatchdogService` after heuristic evaluation. No attribution logic yet — just exact BSSID match as the initial link criterion.

**Phase 6:** Add probabilistic attribution (channel + capability + RSSI fingerprint scoring). Add decay model. Add actor-level reporting to export and timeline UI.

---

## References

- HawkEye 360 "Vessel Custody ID" — persistent vessel identity across AIS rotation (commercial product reference)
- Anduril Lattice `aliases` field — multiple identifiers for the same physical entity (SDK design reference)
- WhoFi arXiv:2507.12869 — RF hardware fingerprinting via CSI (academic basis for hardware-level identity persistence)
- `61_external_research_spire_zainar_he360_anduril_arxiv_2026-05-07.md` — full research context
