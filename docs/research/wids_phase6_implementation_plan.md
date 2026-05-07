# wscan+ Phase 6 WIDS Implementation Plan

_Date: 2026-05-05_

## Purpose

This document converts the recent IDS/WIDS research pass into an implementation plan for the current `dvntone/wscanplus` monorepo.

Canonical repo state is already locked in `docs/SESSION_STATE.md`: Android, desktop, and the web UI/dashboard work all belong under the active `wscanplus` monorepo. The old standalone `dvntone/wscanplus_desktop` repo is archived and is not a live implementation target.

## Current repo shape reviewed

Current canonical shape:

- `android/app` — Android application module.
- `android/core` — Android library module.
- `desktop/` — Electron/Node desktop hub, flat layout after PR #200.
- `desktop/main.js` — Electron main process.
- `desktop/preload.js` — context bridge.
- `desktop/renderer.mjs` — renderer UI.
- `desktop/scanner.mjs` — `iw`-based scan loop.
- `desktop/detector.mjs` — threat scoring.
- `desktop/store.mjs` — EventEmitter state store.
- `desktop/companionServer.mjs` — WebSocket companion server with token auth.
- `desktop/adb/` — ADB transport against `WatchdogService` TCP `:9000`.
- `desktop/companion.js` — companion UI shell.
- `desktop/sessionWorkbench.js` — session artifact import.
- `docs/` — durable project documentation.
- `wscanplus_webui` is active as the darklotuslabs/app web UI placeholder, but implementation planning should keep the monorepo as the canonical source of truth unless a tracked issue says otherwise.

Known implemented state:

- Phase 2 local threat intelligence is complete.
- Seven Android WiFi heuristics are live: WEP/Open, Evil Twin, Encryption Downgrade, Karma, SSID Flooding, RSSI Anomaly, and BSSID Fingerprint.
- `HeuristicEngine`, `PolicyGate`, Room DB, OUI loader, CTI cache, SQLCipher, scan history, GeminiThreatAnalyzer, ADB transport, companion shell, JSON export, capability hello, and floor tracking already exist in some form.
- The locked scanner chain is USB > Standard; root remains explicit dev-mode only; Nexmon and Shizuku are removed.
- Android normal scanning must not be represented as monitor-mode packet capture.

## Research-backed implementation stance

The IDS/WIDS research supports a hybrid approach:

1. Rule/signature detection first.
2. Baseline anomaly scoring second.
3. Dataset/export path third.
4. ML/transformer scoring much later, only after labeled evidence exists.

Do not build a black-box “AI threat detector” before the explainable evidence model is solid.

## Product truth and guardrails

wscan+ should describe itself as:

> An explainable wireless security and signal-intelligence tool that correlates WiFi, BLE, Android companion observations, desktop scans, baselines, and optional monitor-mode evidence to detect suspicious wireless behavior.

Do not claim:

- guaranteed attacker identification;
- exact through-wall location;
- Android monitor-mode packet inspection;
- confirmed deauth detection from normal Android WiFi scans;
- person/device ownership attribution from BLE/BSSID alone.

Use these terms consistently:

- observed;
- correlated;
- suspicious;
- likely;
- needs confirmation;
- confirmed only with packet-level, hardware-backed, or user-confirmed evidence.

## Phase 6 objective

Unify the existing Android heuristics, desktop detector, companion ingestion, scan history, baseline/floor/location signals, and web UI presentation needs into one evidence-first WIDS model.

The goal is not to throw away existing code. The goal is to create a common contract so Android, desktop, and web UI surfaces can produce and display comparable observations, alerts, evidence, limitations, and confidence values.

## Proposed repo additions

### Android

Likely target areas:

- `android/core/src/main/kotlin/com/wscanplus/core/...`
- existing scanner models around `WifiScanResult`.
- existing local threat intelligence / heuristic packages.
- existing Room entities and DAOs.
- `WatchdogService` hello and event serialization.

Add or align:

```kotlin
// Conceptual package name; adapt to current package layout.
com.wscanplus.core.detection.model
com.wscanplus.core.detection.engine
com.wscanplus.core.detection.heuristics
com.wscanplus.core.detection.baseline
com.wscanplus.core.detection.export
```

### Desktop

Likely target files:

- `desktop/detector.mjs`
- `desktop/store.mjs`
- `desktop/scanner.mjs`
- `desktop/companionServer.mjs`
- `desktop/sessionWorkbench.js`
- `desktop/renderer.mjs`
- `desktop/preload.js`

Add or align:

```text
desktop/detection/models.mjs
desktop/detection/rules.mjs
desktop/detection/scoring.mjs
desktop/detection/baseline.mjs
desktop/detection/export.mjs
desktop/test/fixtures/detection/*.json
```

Keep ESM-only. No CommonJS shims.

### Web UI / dashboard surface

The web UI should consume the same normalized alert/evidence model instead of inventing a separate dashboard shape.

Minimum needs:

- shared alert JSON schema;
- confidence/severity display rules;
- limitations display;
- source device labels;
- evidence drilldown;
- no production-only hardcoded demo threat cards.

## Common model contract

### Observation

```ts
type ObservationSource =
  | 'android_wifi'
  | 'android_ble'
  | 'android_sensor'
  | 'desktop_wifi'
  | 'linux_monitor'
  | 'adb'
  | 'manual';

type Observation = {
  id: string;
  source: ObservationSource;
  sourceDeviceId: string;
  timestamp: number;

  ssid?: string;
  bssid?: string;
  frequencyMhz?: number;
  channel?: number;
  rssiDbm?: number;
  capabilities?: string;
  oui?: string;

  bleAddress?: string;
  bleName?: string;
  bleRssiDbm?: number;

  motionState?: 'still' | 'walking' | 'moving' | 'unknown';
  magneticMagnitude?: number;
  floorEstimate?: number;

  scanAgeMs?: number;
  cachedOrThrottled?: 'yes' | 'no' | 'unknown';

  zoneLabel?: string;
  sessionId?: string;
};
```

### Alert

```ts
type AlertType =
  | 'trusted_network_drift'
  | 'suspected_evil_twin'
  | 'suspicious_rogue_ap'
  | 'possible_deauth'
  | 'management_frame_dos'
  | 'recurring_radio_signature'
  | 'baseline_deviation'
  | 'android_scan_limited';

type AlertSeverity = 'info' | 'low' | 'medium' | 'high' | 'critical';

type Alert = {
  id: string;
  type: AlertType;
  severity: AlertSeverity;
  confidence: number;
  title: string;
  explanation: string;
  evidence: EvidenceItem[];
  limitations: string[];
  recommendedAction: string;
  createdAt: number;
  sourceDeviceIds: string[];
  detectorVersion: string;
};
```

### EvidenceItem

```ts
type EvidenceKind =
  | 'ssid_match'
  | 'unknown_bssid'
  | 'security_change'
  | 'channel_change'
  | 'rssi_anomaly'
  | 'multi_sensor_match'
  | 'monitor_frame_event'
  | 'ble_correlation'
  | 'walk_test_inconsistency'
  | 'baseline_missing'
  | 'scan_throttling_possible'
  | 'floor_or_zone_mismatch'
  | 'recurrence';

type EvidenceItem = {
  kind: EvidenceKind;
  weight: number;
  description: string;
  observationIds: string[];
};
```

## Detector rules to implement or align

### 1. Trusted network drift

Inputs:

- trusted AP baseline;
- current WiFi observations;
- OUI/vendor data;
- scan history.

Signals:

- known SSID with unknown BSSID;
- known BSSID with changed capabilities;
- known SSID with security downgrade;
- unexpected channel/frequency;
- RSSI outside baseline range;
- AP disappears/reappears abruptly.

Confidence cap:

- Android-only max: 0.75.
- Can exceed cap only with monitor-mode evidence or user-confirmed router inventory.

### 2. Suspected evil twin

Signals:

- duplicate SSID;
- unknown BSSID;
- security/capability mismatch;
- channel/frequency mismatch;
- unexpected RSSI dominance;
- movement/walk-test inconsistency;
- multi-device observation.

Scoring starter:

```text
+0.30 known SSID with unknown BSSID
+0.20 security/capability change
+0.15 unexpected channel/frequency
+0.15 RSSI anomaly
+0.10 seen by multiple sensors
+0.10 appears during connectivity disruption
+0.25 monitor-mode suspicious frame evidence
```

Label policy:

- Android-only: `Suspected Evil Twin` or `Likely Evil Twin`.
- `Confirmed Evil Twin` requires stronger evidence.

### 3. Suspicious rogue AP

Signals:

- new AP in trusted zone;
- open AP near trusted secure SSID;
- suspicious SSID pattern;
- unknown vendor/OUI;
- persistent high RSSI;
- multi-sensor agreement.

### 4. Possible deauth / management-frame DoS

Android-only evidence is indirect:

- trusted AP disappears abruptly;
- repeated disconnect/reconnect;
- scan volatility spike;
- user-marked disruption.

Monitor-mode evidence:

- deauth/disassoc frame bursts;
- repeated source/target;
- channel-local concentration;
- disconnect correlation.

Confidence cap:

- Android-only max: 0.45.
- High/critical requires monitor-mode source.

### 5. Recurring radio signature

Signals:

- unknown BSSID/BLE appears near user-marked incident windows;
- same signature recurs across days;
- RSSI trend is consistent;
- seen by multiple devices;
- correlates with scan/session events.

Safe wording:

> Recurring nearby radio signature correlated with incidents.

Do not attribute ownership or intent.

### 6. Baseline deviation

Signals:

- RSSI distribution drift;
- expected AP missing;
- channel or security profile drift;
- floor/zone mismatch;
- sudden density increase;
- scan volatility.

## Data persistence plan

Android already has Room. Add/align entities around:

- `ObservationEntity`
- `AlertEntity`
- `EvidenceEntity`
- `BaselineProfileEntity`
- `TrustedApProfileEntity`
- `RadioSignatureEntity`

Desktop can use the existing store first, then persist to JSON or sqlite depending on existing project direction.

Minimum export format:

```json
{
  "schemaVersion": 1,
  "detectorVersion": "phase6-wids-v1",
  "sessionId": "...",
  "observations": [],
  "alerts": [],
  "baselines": [],
  "limitations": []
}
```

## UI implementation plan

Desktop and web UI should show each alert with:

- title;
- severity;
- confidence;
- source devices;
- evidence list;
- limitations;
- recommended action;
- raw observations link.

Android should show compact alerts, but the full evidence view can remain desktop/web-first.

Use clear language:

- `Suspicious AP` instead of `Attacker AP`.
- `Possible deauth-like disruption` instead of `Deauth attack confirmed` when Android-only.
- `Recurring radio signature` instead of `Person/device identified`.

## Testing plan

### Android unit tests

Add fixtures for:

- known SSID + new BSSID;
- security downgrade;
- RSSI anomaly;
- duplicate SSID;
- noisy scan with no alert;
- Android-only deauth-like instability capped at 0.45;
- monitor-mode event lifting confidence.

Run:

```bash
./gradlew :core:test
./gradlew :core:ktlintCheck :app:ktlintCheck
```

### Desktop tests

Add ESM Jest tests for:

- rule scoring;
- confidence caps;
- detector result shape;
- store integration;
- companion payload validation.

Run:

```bash
cd desktop
npm test
```

## Implementation sequence

### PR 1 — Documentation only

Create this document under:

```text
docs/research/wids_phase6_implementation_plan.md
```

No code changes.

### PR 2 — Shared model alignment

- Add/align Android model classes.
- Add/align desktop model module.
- Add JSON schema examples under docs or fixtures.
- No detector behavior changes yet.

### PR 3 — Detector runner alignment

- Add a detector runner interface.
- Normalize existing Android `HeuristicEngine` output into `Alert` + `EvidenceItem`.
- Normalize desktop `detector.mjs` output into same shape.

### PR 4 — Baseline deviation and confidence caps

- Add baseline comparison utilities.
- Enforce Android-only caps.
- Add tests for caps and wording.

### PR 5 — Desktop/web evidence UI

- Add evidence inspector.
- Show limitations and recommended action.
- Stop displaying any hardcoded/demo alerts in production paths.

### PR 6 — Android companion payload enrichment

- Include source capabilities and scan-limitation flags.
- Include device ID and session ID.
- Include floor/zone if available.

### PR 7 — Recurring signature correlation

- Add recurrence tracking.
- Add incident-window correlation.
- Keep language attribution-safe.

### PR 8 — Dataset/export path

- JSONL/CSV export for observations and alerts.
- Include user labels.
- Include detector and baseline version.

## Non-goals for this phase

- No transformer IDS.
- No deep-learning classifier in-app.
- No automatic attacker attribution.
- No exact 3D attacker localization.
- No Android monitor-mode claims.
- No Shizuku/Nexmon reintroduction.
- No CommonJS desktop dependency workaround.

## Review checklist for coding agents

Before opening any implementation PR:

- Read `docs/SESSION_STATE.md`.
- Read `KNOWN_ISSUES.md`.
- Confirm no other PR is open.
- Keep one issue per PR.
- Keep one PR open at a time.
- Do not bypass missing data with demo/static production values.
- Do not weaken permission/scanner guardrails.
- Do not reintroduce archived repo assumptions.
- Do not treat Android companion mode as monitor-mode packet capture.

## Bottom line

The next useful build step is not more research and not ML. It is a normalized evidence contract across Android, desktop, and web UI surfaces:

```text
Observation Store → Baseline Store → Rule Engine Alignment → Evidence UI → Export/Dataset Path
```

This preserves the work already merged while making the product more defensible, testable, and ready for later ML if the data becomes good enough.
