# Phase 6 WIDS Model Alignment Preflight

_Date: 2026-05-05_

Issue: #296  
Depends on: PR #295, `docs/contracts/wids-evidence-contract.md`

## Purpose

This preflight maps the merged Phase 6 WIDS evidence contract onto the current Android and desktop implementation shape before any runtime code changes are attempted.

This is intentionally documentation-only. It does not add model classes, alter detector behavior, change UI, touch Room migrations, or modify scanner/permission/ADB/CTI/Gemini/export behavior.

## Current contract source of truth

The source of truth for Phase 6 evidence output is now:

```text
docs/contracts/wids-evidence-contract.md
```

Representative fixtures live under:

```text
docs/contracts/fixtures/wids/
```

The next implementation should conform existing platform outputs to that contract. It should not replace working scanner, detector, policy, store, or persistence code.

## Files inspected

### Repo state and contract

- `docs/SESSION_STATE.md`
- `docs/contracts/wids-evidence-contract.md`
- `docs/contracts/fixtures/wids/trusted-network-drift.json`
- `docs/contracts/fixtures/wids/phase6-wids-alert-fixtures.json`

### Android

- `android/app/src/main/kotlin/com/wscanplus/app/WatchdogService.kt`
- `android/core/src/main/kotlin/com/wscanplus/core/scanner/WifiScanResult.kt`
- `android/core/src/main/kotlin/com/wscanplus/core/threat/ThreatSignal.kt`
- `android/core/src/main/kotlin/com/wscanplus/core/threat/HeuristicEngine.kt`
- `android/core/src/main/kotlin/com/wscanplus/core/threat/PolicyGate.kt`

### Desktop

- `desktop/detector.mjs`
- `desktop/store.mjs`
- `desktop/scanner.mjs`
- `desktop/companionServer.mjs`

## Confirmed repo shape

`docs/SESSION_STATE.md` remains the canonical state file. It identifies the active repository as `dvntone/wscanplus`, with Android, desktop, and web UI/dashboard work under the monorepo. The old standalone desktop repository is not a live implementation target.

The current monorepo shape relevant to Phase 6 is:

```text
android/app/      Android app and WatchdogService integration
android/core/     Android scanner, threat, and DB core modules
desktop/          Electron/Node desktop hub
web UI/dashboard  Canonical under monorepo unless a tracked issue says otherwise
```

## Android findings

### Existing pipeline

Android already has a working local threat pipeline:

```text
WifiScanResult
  -> toScanInput()
  -> HeuristicEngine.analyze(ScanContext)
  -> PolicyGate.filter(List<ThreatSignal>)
  -> persist ScanResultEntity + ThreatSignalEntity
```

`WatchdogService.kt` wires the seven current local heuristics into `HeuristicEngine`:

```text
WepOpenHeuristic
EvilTwinHeuristic
EncryptionDowngradeHeuristic
KarmaHeuristic
SsidFloodingHeuristic
RssiAnomalyHeuristic
BssidFingerprintHeuristic
```

This means Phase 6 must wrap existing heuristic output. It must not replace the existing Android detector engine.

### Current Android scan model

`WifiScanResult` currently includes:

```text
ssid
bssid
signalLevel
frequencyMhz
capabilities
timestamp
channelWidth
centerFreq0
centerFreq1
```

`toScanInput()` already maps Android scan data into threat-analysis input:

```text
bssid -> bssid
ssid -> ssid
ssid.isBlank() -> isHidden
capabilities -> capabilities
signalLevel -> rssiDbm
frequencyMhz -> frequencyMhz
channelWidth -> channelWidth
timestamp / 1000 -> timestamp
```

This is already close to the Phase 6 `Observation` contract. A future adapter should reuse it.

### Current Android threat model

`ThreatSignal` currently includes:

```text
confidence: Float
source: ThreatSource
reasons: List<String>
heuristicType: HeuristicType?
bssid: String
detectedAt: Long
schemaVersion: Int
```

Current enums:

```text
ThreatSource:
- LOCAL_HEURISTIC
- CROWDSEC_CTI
- GEMINI

HeuristicType:
- WEP_OPEN
- EVIL_TWIN
- ENCRYPTION_DOWNGRADE
- KARMA_ATTACK
- SSID_FLOODING
- RSSI_ANOMALY
- BSSID_FINGERPRINT
```

`ThreatSignal` already enforces confidence in `0.0..0.95` and limits reasons to three. That aligns well with evidence-first output, but it is not yet the full Phase 6 `Alert` / `EvidenceItem` shape.

### Current Android policy gate

`PolicyGate` currently filters signals by minimum confidence:

```text
minimumConfidence = 0.3
```

It does not yet enforce Phase 6 source-aware confidence caps. Future work should add caps as policy constants/tests, not as UI settings.

### Android persistence

`WatchdogService.persistResults()` currently persists:

```text
ScanResultEntity:
- sessionId
- bssid
- ssid
- capabilities
- rssiDbm
- frequencyMhz
- channelWidth
- timestamp
- hidden flag
- location fields

ThreatSignalEntity:
- sessionId
- bssid
- confidence
- source
- heuristicType
- reasons
- detectedAt
- schemaVersion
```

No Room migration should be part of the next implementation unless an explicit issue is opened for persistence changes.

### Android adapter recommendation

Lowest-risk Android implementation point:

```text
After PolicyGate.filter(rawSignals), before export/desktop serialization/UI rendering.
```

Future adapter responsibility:

```text
ThreatSignal + matching WifiScanResult + session/location/floor metadata
  -> Phase 6 Alert + EvidenceItem + Observation-shaped DTO
```

Do not change heuristic scoring in the adapter PR.

## Desktop findings

### Current scan output

`desktop/scanner.mjs` parses `iw` scan output into AP objects:

```text
bssid
ssid
frequency
channel
signal
security
```

Mapping to the Phase 6 contract is straightforward:

```text
frequency -> frequencyMhz
signal -> rssiDbm
security -> capabilities/security summary
```

No scanner behavior change is needed for the first implementation.

### Current detector output

`desktop/detector.mjs` currently returns risk entries shaped like:

```text
bssid
ssid
severity
reasons
confidence
```

The detector is intentionally simple and explainable. It flags:

```text
hidden SSID
open network
WEP
very strong RSSI
SSID collision with different BSSID
```

This should be wrapped into Phase 6 `Alert` / `EvidenceItem`, not rewritten.

### Current store shape

`desktop/store.mjs` keeps:

```text
scanning
scanIntervalMs
interface
aps: Map
riskLog: []
errors: []
companionDevices: Map
```

Risk entries are deduped by:

```text
${entry.bssid}:${entry.severity}
```

and stored with a timestamp.

Future adapter placement can be either:

```text
Option A: scanner.mjs -> scoreAPs() -> contract adapter -> store.addRiskEntries()
Option B: store.addRiskEntries() accepts legacy entries and converts to contract entries internally
```

Prefer Option A. It keeps the store simple and avoids changing store semantics in the first implementation.

### Current companion payload boundary

`desktop/companionServer.mjs` accepts authenticated WebSocket `scan` messages and validates payload shape:

```text
deviceId: string
sequence: integer
timestamp: fresh number
networks: array
```

It also enforces:

```text
token auth
monotonic sequence per device
fresh timestamp window
rate limit: 10 messages per second per device
```

Future companion adapter should treat this as the Android companion ingestion boundary. It should not weaken auth, freshness, monotonic sequence, or rate limits.

## Confidence cap placement

Future confidence cap implementation should live in testable detector policy/constants, not in UI settings.

Contract caps from PR #295:

```text
Android-only suspected evil twin: max 0.75
Android-only possible deauth-like disruption: max 0.45
Android-only recurring radio signature: max 0.65
```

Monitor-mode, hardware-backed, or user-confirmed evidence may lift caps only when represented as explicit evidence.

## What not to do next

Do not do any of the following in the next implementation PR:

```text
Replace Android HeuristicEngine
Replace PolicyGate
Rewrite desktop scanner
Rewrite desktop detector
Change Room schema/migrations
Change Android permissions
Change ADB transport
Change companion auth/rate-limit/freshness guards
Add ML/transformer IDS
Claim Android provides monitor-mode management frames
Add production demo/static alerts
```

## Recommended first true implementation issue

Open a new issue after this preflight:

```text
Phase 6 WIDS contract adapters for Android and desktop
```

Suggested scope:

### In scope

- Add a pure Android mapper from existing scan/threat objects to contract-shaped DTOs.
- Add a pure desktop ESM mapper from existing AP/risk entries to contract-shaped objects.
- Use existing fixtures from `docs/contracts/fixtures/wids/` as examples.
- Add unit tests for mapping only.
- Preserve existing detector behavior.
- Preserve existing store behavior except where needed to call the mapper.

### Out of scope

- No new heuristics.
- No changed confidence scoring.
- No Room migration.
- No UI changes.
- No scanner changes.
- No companion-server auth/validation changes.

## Suggested implementation shape

### Android

Potential package, subject to code review:

```text
android/core/src/main/kotlin/com/wscanplus/core/threat/contract/
```

Potential pure mapper:

```text
ThreatSignalContractMapper
```

Inputs:

```text
ThreatSignal
WifiScanResult or ScanInput
sessionId/deviceId/source metadata
optional location/floor metadata
```

Outputs:

```text
WidsObservationDto
WidsAlertDto
WidsEvidenceItemDto
```

### Desktop

Potential file:

```text
desktop/detection/contractAdapter.mjs
```

Inputs:

```text
AP object from scanner.mjs
risk entry from detector.mjs
source metadata
```

Outputs:

```text
contract-shaped observation + alert + evidence items
```

Tests should assert that existing detector output becomes contract-aligned without changing detector decisions.

## Bottom line

The current code already has the important pieces:

```text
Android: scan model + heuristic engine + policy gate + persistence
Desktop: scan parser + detector + store + companion ingestion guardrails
```

Phase 6 should add adapters around those pieces. It should not rebuild them.
