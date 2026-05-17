# Surveillance AP Heuristic — Feature Concept

**Date:** 2026-05-07  
**Phase target:** Phase 5 / Phase 6  
**Motivation:** WhoFi (arXiv:2507.12869) + DensePose From WiFi (arXiv:2301.00250) — CSI-based passive surveillance via commodity hardware  
**Status:** Concept only. No implementation.

---

## Problem

Current wscanplus heuristics detect APs that are actively attacking (evil twin, karma, SSID flooding, deauth). They do not detect APs that are **passively monitoring** — collecting WiFi Channel State Information (CSI) to track people's presence, movement, and physical identity.

Peer-reviewed research (CMU 2023, La Sapienza 2025) demonstrates this is achievable with two $30–$60 consumer routers and no victim awareness. The attack requires no credential theft, no network intrusion, no associated clients — it is entirely passive and leaves no network-layer trace.

The only observable artifact is the AP's own configuration.

---

## Detection Opportunity

An AP performing CSI-based sensing operates with a characteristic profile driven by the physics of what it is doing:

| Signal | Why it appears | Source |
|--------|---------------|--------|
| Fixed channel, no band steering | Spatial model breaks if channel changes | `frequencyMhz` stable across scans (WifiScanResult) |
| 40 or 80 MHz channel width | More subcarriers → higher spatial resolution | `channelWidth` (WifiScanResult, API 23+) |
| Persistent high RSSI from fixed position | Same hardware, same location across sessions | `signalLevel` variance over time (WifiScanResult) |
| No associated clients (or very few) | Not providing actual internet service | Not in WifiScanResult — requires monitor-mode |
| Near-zero data traffic relative to beacon rate | Beacons are the sensing signal; no data needed | Not in WifiScanResult — requires monitor-mode |
| Elevated beacon rate | More CSI samples per second | Not in WifiScanResult — requires raw frame capture |

Not all signals are available from `WifiScanResult` alone. The most accessible are:
- `frequencyMhz` — channel stability across multiple scans (API 1+)
- `channelWidth` — via `ScanResult.channelWidth` (API 23+)
- `signalLevel` temporal pattern — persistent strong RSSI at fixed position
- No SSID rotation — surveillance APs typically hold a fixed (often generic) SSID

Signals requiring monitor-mode / raw frame capture (not in `WifiScanResult`): beacon interval, associated client count, data-to-beacon traffic ratio.

---

## Proposed Heuristic: `SurveillanceApHeuristic`

### Scoring signals

| Condition | Confidence contribution |
|-----------|------------------------|
| Channel stable across ≥3 consecutive scans | +0.15 |
| Channel width 40 MHz | +0.10 |
| Channel width 80 MHz | +0.15 |
| SSID is generic or blank (no custom branding) | +0.10 |
| RSSI variance < threshold across 5+ scans (stationary device) | +0.15 |
| No associated client devices visible on the same BSSID | +0.10 |
| AP persists for >30 minutes without connecting any clients | +0.15 |
| OUI maps to a consumer router vendor (not corporate AP class) | +0.05 |

Minimum confidence to emit a `ThreatSignal`: 0.40 (two or more signals required).

### Signal source note

Several of these signals require scan history (cross-scan RSSI variance, channel stability over time). This heuristic is therefore blocked by the same `knownProfiles` wiring gap as `BssidFingerprintHeuristic` and `EncryptionDowngradeHeuristic`. It should be implemented after that gap is closed.

---

## Naming and User-Facing Language

**Heuristic tag:** `SURVEILLANCE_AP_CONFIGURATION`

**Tier 1 (Newbie) description:**  
> "A nearby device has the wireless configuration typically used to silently monitor people through walls. It is not providing internet service and may be collecting data about your presence and movements."

**Tier 2 (Advanced) description:**  
> "AP exhibits fixed-channel, wide-bandwidth configuration with no associated clients and persistent RSSI — consistent with WiFi Channel State Information harvesting for passive sensing."

**Tier 3 (Professional) description:**  
> "BSSID \`xx:xx:xx:xx:xx:xx\` matches CSI-sensing profile: channel stable at Xf GHz, 80 MHz width, zero client associations across Y scans, RSSI σ=Z dBm over N minutes. See arXiv:2301.00250, arXiv:2507.12869."

---

## Limitations

- This heuristic produces **circumstantial evidence**, not proof of CSI harvesting. Many legitimate APs (mesh nodes without clients, guest networks, IoT hubs) share some of these characteristics.
- Confidence contributions are intentionally conservative. A single signal should not trigger an alert.
- The heuristic is most useful in combination with `EvilTwinHeuristic` or `KarmaHeuristic` — a surveillance-configured AP that is also impersonating a known SSID is significantly more concerning than one in isolation.

---

## References

- Geng, Huang, De la Torre. "DensePose From WiFi." arXiv:2301.00250. CMU, 2023.
- Avola et al. "WhoFi." arXiv:2507.12869. La Sapienza, 2025.
- `61_external_research_spire_zainar_he360_anduril_arxiv_2026-05-07.md` — full research context
- `62_threat_actor_persistence_layer_concept_2026-05-07.md` — related persistent identity concept
