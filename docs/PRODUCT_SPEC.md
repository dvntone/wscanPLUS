# wscan+ Product Specification

> This document defines the product architecture, output model, and language standards for wscan+.
> All agents must follow this spec when building or modifying UI, reporting, or alert logic.

---

## Core Principle

**One evidence pipeline. Three presentation layers.**

wscan+ maintains a single detection and storage model. Output depth varies by user tier. There are no separate detection engines for different users — only different views of the same evidence.

```
Evidence pipeline (shared)
  └── Observation capture
  └── Anomaly detection
  └── Recurrence correlation
  └── Capability-aware weighting
  └── Session summarization
  └── Export artifacts

Presentation layers (per tier)
  ├── Newbie     — plain language, calm, minimal
  ├── Advanced   — evidence categories, reasons, identifiers
  └── Professional — full fidelity, raw + derived, provenance
```

---

## User Tiers

### Tier 1 — Newbie

**Who:** Elderly residents, nontechnical users, people who need clear summaries without jargon.

**Output style:** Simple status. Plain language. Strong warnings only when evidence is solid.

**Show:**
- Anomaly detected / not detected
- Recurring event / not recurring
- "Suspicious network activity observed"
- "Investigation recommended"
- Visual timeline and map emphasis
- Export/report button

**Hide by default:**
- MAC addresses
- Certificate chain details
- RSSI spread values
- Raw heuristic confidence scores
- Provider-specific markers
- Debug output

---

### Tier 2 — Advanced

**Who:** Security-aware users, technical tenants, people comfortable with logs but not full analyst workflows.

**Output style:** Show evidence categories and reasons. Surface identifiers. Show capability state.

**Show:**
- Heuristic reasons (plain English)
- Recurrence count and window
- Source device
- Environment type
- Baseline deviation
- Portal comparison result (if applicable)
- BLE anomaly summary
- Capability / ADB-assisted / degraded mode status
- Export as JSON or CSV

**Hide by default:**
- Full debug traces
- All raw scan records
- Internal model metadata
- Verbose correlation internals

---

### Tier 3 — Professional

**Who:** The developer, security researchers, analysts, power users validating behavior.

**Output style:** Full fidelity. Raw and derived data. Provenance and uncertainty included.

**Show:**
- All threat signals with confidence values
- All reasons with provenance
- Timestamps and identifiers
- Historical context and recurrence windows
- Portal fingerprints (redirect chain, TLS, structural markers)
- BLE anomaly raw counts
- Capability manifest state
- ADB-assisted / native / degraded classification per feature
- Export artifacts (JSON, PCAP reference)
- Test and diagnostic output

---

## Language Rules

### Use these terms

| Term | When to use |
|---|---|
| `observation` | Something was detected and recorded |
| `anomaly` | A deviation from the established baseline |
| `recurring anomaly` | Same pattern detected across multiple sessions |
| `correlated event` | Multiple signals pointing to the same cause |
| `suspicious portal` | Captive portal that does not match known-good baseline |
| `investigation recommended` | Evidence warrants further review but is not confirmed |
| `enhanced mode available` | ADB-assisted features can improve detection on this device |
| `ADB-assisted mode required` | This feature requires ADB on this device |
| `degraded mode` | Running with reduced capability (e.g. fine location denied, background access unavailable) |

### Avoid these terms

| Term | Why |
|---|---|
| `attacker confirmed` | Attribution is an inference, not a measured fact |
| `attacker detected` | Same — overstates certainty |
| `compromised` | Too absolute for the evidence level this app produces |
| `hidden camera detected` | Not a claim this evidence pipeline can support |
| `BLE spam tool confirmed` | Tooling type is inference, not observation |
| `Flipper / ESP32 detected` | Device class cannot be confirmed from BLE advertisement data alone |
| `resident confirmed` | Personal attribution from RF evidence alone is not defensible |
| `proven` | wscan+ produces evidence, not legal proof |

**General rule:** wscan+ is an evidence collection and analysis tool, not an attribution engine. Language must reflect what was observed and measured, not what was inferred about intent or actor identity. Reports must be defensible to nontechnical stakeholders, building management, and support services.

---

## Minimum Evidence Requirements

Before expanding UI or alerting features, the pipeline must reliably produce all five evidence objects:

### 1. Observation
- What happened (heuristic type, signal category)
- When (timestamp)
- Where (GPS coordinates if available, floor estimate if barometer present)
- Which device saw it (device serial, capability state)

### 2. Anomaly
- What deviated from the baseline
- Why it deviated (reason strings, max 3 per signal)
- Whether recurrence exists across sessions

### 3. Capability status
- What this device can actually do natively
- Which features require ADB-assisted mode
- Which features are enhanced by ADB-assisted mode
- Which features are degraded or unavailable
- `WORKS_NATIVE / WORKS_ADB / WORKS_ADB_ENHANCED / WORKS_DEGRADED / FAILS / UNTESTED` per feature

### 4. Session summary
- Key events from the session
- Environment context
- Confidence level of findings
- Exportable report artifact

### 5. Cross-session recurrence
- Same anomaly pattern seen before?
- Same location?
- Same time window?
- Same source device(s)?

**Gate condition:** If these five evidence objects are not yet reliably produced, UI should remain minimal. Do not build polished alert dashboards or advanced correlation views before the underlying data is trustworthy.

---

## UI Build Sequencing

### Build now (data is sufficient)
- Diagnostics / capability screen
- Session summary screen
- Scan history timeline
- Map / heatmap overlay
- Export / report screen

### Defer until evidence objects are validated
- Polished multi-tier alert dashboards
- Advanced campaign or actor views
- Complicated cross-session correlation UI
- Portal or BLE specialized screens
- Desktop orchestration UI

---

## Detection Output Classification

Capability status labels (used in reporting and UI):

| Label | Meaning |
|---|---|
| `WORKS_NATIVE` | Feature functions without ADB or special permissions |
| `WORKS_ADB` | Feature functions only with ADB-assisted mode active |
| `WORKS_ADB_ENHANCED` | Feature functions natively but improves significantly with ADB |
| `WORKS_DEGRADED` | Feature runs but with reduced data quality or coverage |
| `FAILS` | Feature does not function on this device/OS combination |
| `UNTESTED` | Not yet validated on this device |
