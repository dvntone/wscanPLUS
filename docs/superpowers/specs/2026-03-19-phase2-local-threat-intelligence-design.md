# Phase 2: Local Threat Intelligence — Design Spec

> Approach B: Logic First, Stub Data. All heuristic logic implemented and tested as pure Kotlin before Room/KSP integration.

## What This Does (Plain English)

Phase 2 adds the brain that decides whether a nearby WiFi network is suspicious. It runs entirely on the phone — no internet needed, no data leaves the device. Every time the scanner picks up nearby networks, seven different checks run against the results. Each check produces a confidence score (0–95%) and plain-English reasons explaining why something looks suspicious.

Later phases add external intelligence (CrowdSec) and AI analysis (Gemini) on top of these local scores.

## Architecture

```
Scanner Results (from WatchdogService)
        |
        v
  OUI Vendor Lookup (asset file)
        |
        v
  Heuristic Engine (7 checks, sequential — parallelism deferred)
        |
        v
  ThreatSignal list (score + reasons per check)
        |
        v
  Policy Gate (threshold filter + false-positive brakes)
        |
        v
  Filtered ThreatSignals → stored in Room DB (Phase 2 PR #7/8)
                         → surfaced to UI (Phase 4)
```

All components live in `:core` module. No Android framework dependencies in heuristic logic — pure Kotlin, fully unit-testable.

## Data Models

All in `com.wscanplus.core.threat`.

### ThreatSource
```kotlin
enum class ThreatSource { LOCAL_HEURISTIC, CROWDSEC_CTI, GEMINI }
```

### HeuristicType
```kotlin
enum class HeuristicType {
    WEP_OPEN, EVIL_TWIN, ENCRYPTION_DOWNGRADE, KARMA_ATTACK,
    SSID_FLOODING, RSSI_ANOMALY, BSSID_FINGERPRINT
}
```

### SecurityType
```kotlin
enum class SecurityType(val rank: Int) {
    WPA3(4), OWE(3), WPA2(3), WPA(2), WEP(1), OPEN(0)
}
```

### ThreatSignal
```kotlin
data class ThreatSignal(
    val confidence: Float,              // 0.0–0.95 (never 1.0)
    val source: ThreatSource,
    val reasons: List<String>,          // top 3 human-readable
    val heuristicType: HeuristicType?,  // null for CTI/Gemini signals
    val bssid: String,
    val detectedAt: Long = System.currentTimeMillis(),
    val schemaVersion: Int = 1          // enables future migration without Room schema changes
)
```

### BssidProfile (in-memory fingerprint, Room annotations added later)
```kotlin
data class BssidProfile(
    val bssid: String,
    val firstSeenAt: Long,
    val lastSeenAt: Long,
    val ssids: Set<String>,                 // immutable — mutate via copy()
    val capabilitiesHistory: List<String>,   // immutable — mutate via copy()
    val observationCount: Int,
    val ouiVendor: String?
)
```

Mutation pattern: `profile.copy(ssids = profile.ssids + newSsid, observationCount = profile.observationCount + 1)`. This preserves stable `equals`/`hashCode` for map lookups.

### ScanContext (input wrapper for heuristic engine)
```kotlin
data class ScanContext(
    val currentResults: List<ScanInput>,
    val knownProfiles: Map<String, BssidProfile>,
    val baselineNetworkCount: Int?,     // null if < MIN_BASELINE_SAMPLES sessions observed
    val baselineStdDev: Double?,        // null if < MIN_BASELINE_SAMPLES; needed for z-score
    val environmentType: EnvironmentType
)

data class ScanInput(
    val bssid: String,
    val ssid: String,
    val isHidden: Boolean,              // derived: ssid.isBlank()
    val capabilities: String,
    val rssiDbm: Int,
    val frequencyMhz: Int,
    val channelWidth: Int,
    val timestamp: Long
)

enum class EnvironmentType { RESIDENTIAL, OFFICE, PUBLIC }
```

**Why `ScanInput` exists separately from `WifiScanResult`:** Decouples `com.wscanplus.core.threat` from `com.wscanplus.core.scanner`. Heuristic logic depends only on its own input types, not scanner implementation details. The mapping is a `WifiScanResult.toScanInput()` extension function defined in the WatchdogService wiring PR (#9).

**`EnvironmentType` default:** Defaults to `RESIDENTIAL` until a user-configurable setting is added in Phase 4 UI. The WatchdogService wiring PR passes `EnvironmentType.RESIDENTIAL` unless overridden.

### Baseline Network Count

`baselineNetworkCount` is the **mean distinct SSID count per scan session**, computed from stored scan history.

- Maintained as a running average in the `BssidProfile` store (in-memory map during Phase 2, Room query after PR #7/8)
- Updated after each scan: `(previousMean * (n-1) + currentCount) / n`
- Returns `null` until `MIN_BASELINE_SAMPLES` (10) sessions have been recorded
- Used by SSID Flooding heuristic for z-score calculation
- The standard deviation is tracked alongside the mean for z-score: `z = (currentCount - mean) / stddev`

## Heuristic Engine

### Interface
```kotlin
interface Heuristic {
    val type: HeuristicType
    fun evaluate(input: ScanInput, context: ScanContext): ThreatSignal?
}
```

Each heuristic returns `null` if no threat detected, or a `ThreatSignal` with confidence and reasons.

### Engine Coordinator
```kotlin
class HeuristicEngine(private val heuristics: List<Heuristic>) {
    fun analyze(context: ScanContext): List<ThreatSignal> {
        return context.currentResults.flatMap { input ->
            heuristics.mapNotNull { it.evaluate(input, context) }
        }
    }
}
```

### The 7 Heuristics

**1. WEP/Open Detection** (`WepOpenHeuristic`)
- Parses `capabilities` string for security type
- Confidence: OPEN=0.4, WEP=0.6, WPA=0.2, OWE/WPA2/WPA3=0.0
- OWE (Enhanced Open) is NOT a threat
- Captive portals are legitimate Open networks

**2. Evil Twin** (`EvilTwinHeuristic`)
- Same SSID + different BSSID alone is NOT enough (enterprise multi-AP is normal)
- 5 sub-signals combined: OUI vendor mismatch (+0.3), security capability mismatch (+0.35), same channel (+0.1), new BSSID for established SSID (+0.2), RSSI anomaly vs average (+0.15)
- Guest networks: BSSIDs sharing first 5 octets = same hardware = not evil twin
- Combined confidence via `1 - (1-a)(1-b)...` formula, capped at 0.95
- OUI vendor lookup is optional (`OuiLookup?`) — when null, OUI mismatch sub-signal is skipped (0.0 contribution). Full vendor detection activates after OUI PR.

**3. Encryption Downgrade** (`EncryptionDowngradeHeuristic`)
- Compares current security type vs historical from BssidProfile
- Rank comparison: WPA3=4, OWE/WPA2=3, WPA=2, WEP=1, OPEN=0
- Confidence: WPA2/3->Open=0.85, WPA2/3->WEP=0.75, WPA->Open=0.5
- Same-rank transitions (e.g., OWE<->WPA2) are NOT downgrades — no signal emitted
- Minimum history required: 3 sessions across 2+ days before flagging
- Kismet calls this CRYPTODROP

**4. Karma Attack** (`KarmaHeuristic`)
- Multiple distinct SSIDs sharing single BSSID = WiFi Pineapple behavior
- Threshold: 3+ SSIDs=0.45, 5+=0.75, 10+=0.95
- Same channel + tight RSSI (<=6dBm range) increases confidence
- Excludes hidden SSIDs (empty string) from count

**5. SSID Flooding** (`SsidFloodingHeuristic`)
- Z-score vs rolling baseline (needs EnvironmentType context)
- Z>=5.0 or absolute>200: confidence 0.90
- Z>=3.5: 0.70, Z>=2.5: 0.50
- Requires MIN_BASELINE_SAMPLES=10 sessions before activating
- Normal ranges: rural 0-5, suburban 5-20, urban 15-50, office 20-80

**6. RSSI Anomaly** (`RssiAnomalyHeuristic`)
- Unusually strong signal for a new/unknown network
- Thresholds by environment: -30dBm residential, -25 office, -20 public
- New+strong: >-20dBm=0.80, >-25=0.60, >-30=0.45
- Known+strong: lower confidence (0.30 for >-20dBm)

**7. BSSID Fingerprinting/Rotation** (`BssidFingerprintHeuristic`)
- Track new BSSIDs appearing for established SSIDs
- Rotation detection: old BSSID disappears + new appears within 30min window
- OUI overlap: same vendor = lower confidence (firmware), different = higher
- Locally-administered MAC (bit 1 of first octet set) = 0.25 signal alone
- Infrastructure APs almost never use locally-administered MACs
- OUI vendor lookup is optional (`OuiLookup?`) — when null, OUI overlap check is skipped. Full vendor detection activates after OUI PR.

### Confidence Philosophy
- Never 1.0 — cap at 0.95
- Multiple independent signals: `1 - (1-a)(1-b)` formula
- False positive cost is high — err toward lower confidence

## OUI Vendor Lookup

```kotlin
class OuiLookup(private val ouiMap: Map<String, String>) {
    fun lookup(bssid: String): String?
    fun isSuspiciousVendor(bssid: String): Boolean
    fun isLocallyAdministered(bssid: String): Boolean
}
```

- Loaded from `oui.csv` bundled as Android asset (~2MB, ~35,000 entries)
- Key: first 3 octets of BSSID uppercase without colons (e.g., "B827EB")
- Suspicious vendor set (~20 OUI prefixes): Raspberry Pi, Espressif, Flipper, Alfa Network
- Locally-administered MAC: `(firstByte AND 0x02) != 0`
- Parsing is a standalone utility — no Android dependency for the lookup logic itself; only the asset loading needs Android Context

## Policy Gate

```kotlin
class PolicyGate(private val config: PolicyConfig) {
    fun filter(signals: List<ThreatSignal>): List<ThreatSignal>
}

data class PolicyConfig(
    val minimumConfidence: Float = 0.3f,
)
```

- Filters out signals below minimum confidence threshold
- Passes through signals that exceed threshold for potential CTI/Gemini escalation (Phase 3/4)
- Note: `falsePositiveBrakes` was removed — it was never implemented and had no effect on filtering

## WatchdogService Integration

The existing `WatchdogService.onScanResults()` callback currently receives scan results and does nothing with them beyond logging. Phase 2 wiring:

1. Convert `WifiScanResult` list to `ScanInput` list
2. Build `ScanContext` from current results + stored profiles
3. Run `HeuristicEngine.analyze(context)`
4. Pass results through `PolicyGate.filter()`
5. Store filtered `ThreatSignal` list (in-memory initially, Room in PR #7/8)
6. Update `BssidProfile` map with new observations

## Testing Strategy

Every heuristic gets dedicated unit tests in `:core` module. No Android dependencies needed — all pure Kotlin.

**Test categories per heuristic:**
- Happy path: clear threat signal produced with expected confidence
- Negative: benign input produces null (no false positive)
- Edge cases: boundary conditions specific to that heuristic
- Confidence math: verify combined scoring formula

**Specific edge cases to test:**
- WEP/Open: OWE is NOT flagged; captive portal handling
- Evil Twin: enterprise multi-AP (same SSID, same OUI) NOT flagged; guest network detection via shared first 5 octets
- Encryption Downgrade: insufficient history (< 3 sessions) returns null
- Karma: hidden SSIDs excluded from count; exactly 3 threshold
- SSID Flooding: insufficient baseline (< 10 sessions) returns null
- RSSI: environment type affects threshold correctly
- BSSID Fingerprint: locally-administered MAC detection; 30min rotation window

**OUI tests:** lookup correctness, suspicious vendor matching, locally-administered MAC bit check

**Policy Gate tests:** below-threshold filtering, false-positive brake conditions, empty input

## PR Sequence (~9 PRs)

| PR | Contents | Tests |
|----|----------|-------|
| 1 | ThreatSignal, ThreatSource, HeuristicType, SecurityType, ScanContext, ScanInput, BssidProfile, EnvironmentType data models | Model construction, confidence bounds, schemaVersion default |
| 2 | SecurityType parser + WepOpenHeuristic | Parser coverage, OWE not flagged, confidence values |
| 3 | EvilTwinHeuristic + EncryptionDowngradeHeuristic (OuiLookup? = null) | Multi-AP not flagged, sub-signal combination, same-rank no-op, history minimum |
| 4 | KarmaHeuristic + SsidFloodingHeuristic | Hidden SSID exclusion, z-score thresholds, baseline minimum, isHidden flag |
| 5 | RssiAnomalyHeuristic + BssidFingerprintHeuristic (OuiLookup? = null) | Environment thresholds, MAC bit check, rotation window |
| 6 | OUI asset loader + OuiLookup + suspicious vendor set | Lookup correctness, suspicious matching, locally-administered |
| 7 | Room + KSP setup: DB class, 5 entities, TypeConverters | Entity construction, TypeConverter round-trips |
| 8 | Room DAOs (5 DAOs) + in-memory DB integration tests | DAO queries via Room in-memory test DB |
| 9 | PolicyGate + HeuristicEngine coordinator + WatchdogService wiring + `WifiScanResult.toScanInput()` mapping | Threshold filtering, false-positive brakes, end-to-end flow |

Each PR: <200 LOC (tests excluded), one concern, references one GitHub issue.

**OUI dependency note:** PRs #3 and #5 accept `OuiLookup?` (nullable). When null, OUI-dependent sub-signals contribute 0.0. After PR #6 merges, OuiLookup is injected and those sub-signals activate. No code changes needed in the heuristics — the null-check is built into the logic.

## What This Does NOT Include

- No UI (Phase 4)
- No CrowdSec CTI calls (Phase 3 — needs consent framework first)
- No Gemini/firebase-ai calls (Phase 4)
- No multi-device support (Phase 5)
- No export functionality (Phase 4)

## Schema Decisions

- `schemaVersion` field: added to ThreatSignal data model as `val schemaVersion: Int = 1` — enables future migration without Room schema changes
- `deviceId`: deferred to Phase 3 (multi-device architecture locked, implementation later)
- Room `fallbackToDestructiveMigration()` during dev — no migration files until Phase 6 release prep
