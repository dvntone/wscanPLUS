# External Research — Project-Applicable Findings

**Date:** 2026-05-07  
**Sources:** spire.com · zainartech.com · he360.com · anduril.com · arXiv:2507.12869 · arXiv:2301.00250  
**Scope:** Deep research pass; findings filtered for wscanplus applicability

---

## 1. Anduril Industries — Lattice SDK

**URL:** developer.anduril.com  
**Relevance: HIGH**

### Summary
Anduril's Lattice is a defense-grade platform for multi-sensor data fusion and autonomous coordination. The Lattice SDK is fully public and provides REST and gRPC APIs for building sensor integrations.

### Applicable Concepts

**Entity + Provenance model**  
Each Lattice entity carries:
- `entity_id` — stable UUID for the entity
- `aliases` — multiple identifiers for the same physical object (covers identifier rotation)
- `provenance` — which sensor produced this, at what time, with what confidence
- `ontology` — type classification

This is more mature than wscanplus's current flat `ThreatSignal` model. Gaps:
- No `provenance.deviceId` (which Android device observed this signal)
- No `aliases` for BSSID variants of the same physical AP
- No cross-session entity identity

**Confidence propagation**  
Lattice tracks confidence per source per entity update, enabling downstream consumers to weight signals by source reliability. Maps directly to the unimplemented confidence propagation requirement in `docs/SESSION_STATE.md`.

**Persistent track identity**  
Lattice maintains entity tracks that survive identifier changes (AIS MMSI spoofing, transponder failures). wscanplus needs the equivalent: a `threatActorId` UUID persisting across BSSID rotations. See `62_threat_actor_persistence_layer_concept_2026-05-07.md`.

**AIS Integration Sample App**  
`github.com/anduril/sample-app-ais-integration-rest` — demonstrates ingesting external sensor streams and publishing as Lattice entities. AIS spoofing detection in that app is architecturally identical to BSSID/SSID spoofing detection. Useful structural reference for the desktop hub aggregation layer.

**gRPC vs REST justification**  
Lattice v1 = native gRPC; v2 = OpenAPI (REST/JSON). Stated reason: REST for web apps and rapid development, gRPC for bandwidth-constrained hardware integrations. Directly applies to wscanplus's current WebSocket/JSON transport vs. a potential future protobuf binary framing.

### Actionable Items
- Extend `ThreatSignal` with `provenance.deviceId` and `provenance.sensorType` (Phase 5)
- Add `bssidAliases: List<String>` to the threat track data model — closes the `knownProfiles` gap for `BssidFingerprintHeuristic` (Phase 5)
- Use the Lattice entity+provenance pattern as the reference model for the desktop hub's unified timeline data model

---

## 2. ZaiNar Technologies — Physical AI / Wireless Positioning

**URL:** zainartech.com  
**Relevance: HIGH (Phase 6+)**

### Summary
Sub-meter indoor/outdoor positioning using existing WiFi and cellular infrastructure. Core innovation: sub-nanosecond time synchronization on commodity networks, enabling TDOA calculations that locate devices through walls with sub-meter accuracy. No dedicated hardware, no GPS.

### Applicable Concepts

**Rogue AP triangulation**  
If 2+ Android devices observe the same BSSID with RSSI + synchronized timestamps, the physical position of a rogue AP can be estimated using TDOA. This answers "where in the building is the attacker" — currently missing from wscanplus's evidence output.  
Prerequisites: multi-device correlation (Phase 5), explicit clock synchronization across devices. Note: `WifiScanResult.timestamp` is microseconds since each device's individual boot — these values are not comparable across devices without a shared time reference. Practical TDOA requires either NTP-aligned wall-clock timestamps or WiFi RTT/FTM (`WifiRttManager`, Android 9+) as the ranging primitive instead.

**WiFi Fine Timing Measurement (FTM / IEEE 802.11mc)**  
Android 9+ has `WifiRttManager` for hardware-level ranging (sub-meter accuracy, ~1–2m typical indoors). This is a Phase 6 natural extension for rogue AP triangulation — complements `BarometerSampler` altitude with horizontal position.

**RSSI anomaly false positive reduction**  
ZaiNar's work validates that many RSSI variations that look like anomalies have predictable physical explanations (multipath, obstruction, body shadowing). A simple propagation model (inverse-square + wall attenuation factor) in `RssiAnomalyHeuristic` would reduce false positives from legitimate environmental variability.

**Offline-first validation**  
ZaiNar's "no GPS, no cloud" positioning approach validates wscanplus's offline-first architecture philosophy. No changes needed.

### No API Integration
ZaiNar has no public API. Engagement is sales-driven. The value is in methodology, not data integration.

---

## 3. HawkEye 360 — RF Geolocation via Satellite

**URL:** he360.com  
**Relevance: MODERATE (methodology only)**

### Summary
Commercial satellite constellation geolocating RF emitters globally via TDOA/FDOA triangulation. Primary market: maritime domain awareness, AIS spoofing detection, dark vessel tracking.

### Applicable Concepts

**AIS spoofing = BSSID/SSID spoofing (structural equivalence)**  

| HawkEye 360 | wscanplus |
|-------------|-----------|
| Vessel broadcasting false AIS ID | AP broadcasting cloned/spoofed BSSID |
| Dark vessel (AIS off) hiding in traffic | Rogue AP with rotating MAC |
| RF fingerprint persists across AIS outages | Hardware RF signature persists across MAC rotation |
| Multi-satellite TDOA triangulation | Multi-device RSSI triangulation |
| Formation-flying sensor clusters | Distributed Android scanner nodes |

**"Vessel Custody ID" concept**  
HawkEye's AI product maintains a persistent tracking ID for a vessel using RF fingerprint features (modulation signature, timing, emission pattern) that survive MMSI rotation. This is exactly what wscanplus needs: a persistent `ThreatActorEntity` UUID that survives BSSID rotation by fingerprinting the attacker's radio hardware.  
See `62_threat_actor_persistence_layer_concept_2026-05-07.md`.

### No API Integration
HawkEye data covers global satellite RF signals — not relevant to in-building WiFi detection.

---

## 4. Spire Global — Satellite Data APIs

**URL:** spire.com, developers.wx.spire.com  
**Relevance: LOW — no action**

Weather, aviation ADS-B, atmospheric data via REST APIs. No meaningful overlap with WiFi threat detection at residential scale. No integration recommended.

Possible speculative future use: outdoor temperature/pressure for altitude calibration supplementing `BarometerSampler` — but this is overengineering for the residential threat detection context.

---

## 5. arXiv:2507.12869 — WhoFi: Person Re-ID via WiFi CSI

**Citation:** Avola et al., La Sapienza University of Rome. "WhoFi: Deep Person Re-Identification via Wi-Fi Channel Signal Encoding." arXiv:2507.12869, July 2025.  
**Relevance: CRITICAL**

### Finding
Standard WiFi hardware captures **biometric information** in Channel State Information (CSI) — the amplitude and phase of individual 802.11 subcarriers. A Transformer network trained on CSI variations re-identifies specific individuals with **95.5% Rank-1 accuracy** across multiple clothing scenarios, through walls, using two consumer routers.

### Threat Model Impact
A rogue AP can **biometrically fingerprint residents** — confirming "target is home," distinguishing resident A from resident B — using $30–$60 hardware, no cameras, no audio, no victim awareness. This is a new attack class documented in `THREAT_CONTEXT.md`.

### RF Hardware Fingerprinting (inverse application)
The same physical phenomenon that enables person Re-ID means each WiFi radio has hardware imperfections (IQ imbalance, carrier frequency offset, analog front-end characteristics) that appear as unique patterns in its CSI emissions. An attacker who rotates their BSSID still emits from the same chipset. When desktop monitor-mode capture is available (Kismet integration, Phase 5/6), hardware-level RF fingerprinting can identify the attacker's physical radio across MAC rotations.

**This extends `BssidFingerprintHeuristic` beyond what scan results alone can achieve.**

### New Heuristic Concept — Surveillance AP Configuration
An AP performing CSI-based sensing uses a characteristic operating profile. Observable from current `WifiScanResult` fields: fixed `frequencyMhz` across scans, wide `channelWidth` (API 23+), low RSSI variance over time. Requires monitor-mode/extra telemetry: elevated beacon rate, associated client count, data-to-beacon traffic ratio. See `63_surveillance_ap_heuristic_concept_2026-05-07.md` for full signal breakdown.

---

## 6. arXiv:2301.00250 — DensePose From WiFi

**Citation:** Geng, Huang, De la Torre. "DensePose From WiFi." arXiv:2301.00250. Carnegie Mellon University, 2023.  
**Relevance: HIGH**

### Finding
Standard 802.11 routers (~$60 for two) generate **full-body 3D pose maps** of people — posture, limb positions, activity patterns, movement tracking — using only CSI amplitude and phase. No cameras. Performance approaches image-based pose estimation in the same environment (43.5 AP vs 84.7 AP for cameras).

### Threat Model Impact
Combined with WhoFi, a rogue AP can simultaneously perform credential theft and:
1. Detect presence (is target home?)
2. Track movement (where in the space?)
3. Estimate activity (sleeping, sitting, in bathroom, at door)
4. Re-identify individuals
5. Build a behavioral profile across days/weeks

This is documented in `THREAT_CONTEXT.md`.

### Calibration Requirement = Behavioral Indicator
DensePose WiFi requires the attacker to be in a relatively fixed position during model calibration — they cannot be roaming around the building. A **sustained, stationary, unconfigured AP near a residential unit** is more suspicious than a transient one. This behavioral pattern is already partially covered by `EvilTwinHeuristic` (same BSSID persisting over time) but could be explicitly incorporated into threat scoring.

### Evidence Framing Upgrade
The CMU DensePose paper provides a citable peer-reviewed source for the claim that $60 hardware can track movement through walls. This is not a theoretical argument — it changes the weight of the evidence wscanplus generates when shared with building management or law enforcement.

---

## Priority Summary

| Action | Phase | Filed |
|--------|-------|-------|
| THREAT_CONTEXT.md — add CSI surveillance section | Immediate | Done (2026-05-07) |
| Threat Actor Persistence Layer concept | Phase 5/6 | `62_...` |
| Surveillance AP Heuristic concept | Phase 5/6 | `63_...` |
| Extend ThreatSignal with provenance fields | Phase 5 | — |
| Add bssidAliases to threat track data model | Phase 5 | — |
| WiFi FTM (WifiRttManager) for rogue AP ranging | Phase 6+ | — |
