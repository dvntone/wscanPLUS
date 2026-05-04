# Phase 2: Local Threat Intelligence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add on-device WiFi threat detection (7 heuristics) to the Android companion app — no internet required, no data leaves the device.

**Architecture:** Pure Kotlin heuristic logic in `:core` module, tested with JUnit. Room database added after logic is proven. WatchdogService wired last.

**Tech Stack:** Kotlin (AGP 9.1.0 built-in), JUnit 4.13.2, Room 2.8.4 + KSP 2.2.10-2.0.2 (PR 7-8 only)

**Spec:** `docs/superpowers/specs/2026-03-19-phase2-local-threat-intelligence-design.md`

---

## File Map

### New files (`:core` module, all under `android/core/src/main/kotlin/com/wscanplus/core/`)

| File | Responsibility |
|------|---------------|
| `threat/ThreatSignal.kt` | ThreatSignal, ThreatSource, HeuristicType data classes |
| `threat/SecurityType.kt` | SecurityType enum + capabilities parser |
| `threat/ScanContext.kt` | ScanContext, ScanInput, EnvironmentType, BssidProfile |
| `threat/Heuristic.kt` | Heuristic interface |
| `threat/HeuristicEngine.kt` | Engine coordinator |
| `threat/WepOpenHeuristic.kt` | Heuristic 1 |
| `threat/EvilTwinHeuristic.kt` | Heuristic 2 |
| `threat/EncryptionDowngradeHeuristic.kt` | Heuristic 3 |
| `threat/KarmaHeuristic.kt` | Heuristic 4 |
| `threat/SsidFloodingHeuristic.kt` | Heuristic 5 |
| `threat/RssiAnomalyHeuristic.kt` | Heuristic 6 |
| `threat/BssidFingerprintHeuristic.kt` | Heuristic 7 |
| `threat/PolicyGate.kt` | PolicyGate + PolicyConfig |
| `threat/OuiLookup.kt` | OUI vendor lookup (pure logic, no Android deps) |
| `threat/OuiAssetLoader.kt` | Android asset loading for oui.csv (needs Context) |
| `db/WscanDatabase.kt` | Room database class (PR 7) |
| `db/entity/ScanSessionEntity.kt` | Room entity (PR 7) |
| `db/entity/ScanResultEntity.kt` | Room entity (PR 7) |
| `db/entity/BssidFingerprintEntity.kt` | Room entity (PR 7) |
| `db/entity/ThreatSignalEntity.kt` | Room entity (PR 7) |
| `db/entity/CtiCacheEntity.kt` | Room entity (PR 7) |
| `db/Converters.kt` | Room TypeConverters (PR 7) |
| `db/dao/ScanSessionDao.kt` | Room DAO (PR 8) |
| `db/dao/ScanResultDao.kt` | Room DAO (PR 8) |
| `db/dao/BssidFingerprintDao.kt` | Room DAO (PR 8) |
| `db/dao/ThreatSignalDao.kt` | Room DAO (PR 8) |
| `db/dao/CtiCacheDao.kt` | Room DAO (PR 8) |

### New test files (all under `android/core/src/test/kotlin/com/wscanplus/core/`)

| File | Tests |
|------|-------|
| `threat/ThreatSignalTest.kt` | Model construction, defaults, confidence bounds |
| `threat/SecurityTypeTest.kt` | Parser coverage for all capability strings |
| `threat/WepOpenHeuristicTest.kt` | WEP/Open/OWE/WPA confidence values |
| `threat/EvilTwinHeuristicTest.kt` | Sub-signal combination, multi-AP negative, guest network |
| `threat/EncryptionDowngradeHeuristicTest.kt` | Rank comparison, same-rank no-op, history minimum |
| `threat/KarmaHeuristicTest.kt` | SSID count thresholds, hidden SSID exclusion |
| `threat/SsidFloodingHeuristicTest.kt` | Z-score thresholds, baseline minimum |
| `threat/RssiAnomalyHeuristicTest.kt` | Environment thresholds, known vs new |
| `threat/BssidFingerprintHeuristicTest.kt` | MAC bit check, rotation window, OUI overlap |
| `threat/OuiLookupTest.kt` | Lookup, suspicious vendor, locally-administered |
| `threat/PolicyGateTest.kt` | Threshold filtering |
| `threat/HeuristicEngineTest.kt` | Coordinator wiring, empty input |

### Modified files

| File | Change |
|------|--------|
| `android/build.gradle.kts` | Add KSP + Room plugin declarations (PR 7) |
| `android/core/build.gradle.kts` | Add KSP + Room plugins + deps (PR 7) |
| `android/app/src/main/kotlin/com/wscanplus/app/WatchdogService.kt` | Wire heuristic engine into scan callback (PR 9) |
| `android/core/src/main/kotlin/com/wscanplus/core/scanner/WifiScanResult.kt` | Add `toScanInput()` extension (PR 9) |
| `android/app/src/main/assets/oui.csv` | Bundled OUI database file (PR 6) |

---

## Task 1: Data Models (PR #1)

**Branch:** `claude/p2-threat-data-models`
**Issue:** Create before starting

**Files:**
- Create: `android/core/src/main/kotlin/com/wscanplus/core/threat/ThreatSignal.kt`
- Create: `android/core/src/main/kotlin/com/wscanplus/core/threat/SecurityType.kt`
- Create: `android/core/src/main/kotlin/com/wscanplus/core/threat/ScanContext.kt`
- Create: `android/core/src/test/kotlin/com/wscanplus/core/threat/ThreatSignalTest.kt`

- [ ] **Step 1: Create `ThreatSignal.kt`**

```kotlin
package com.wscanplus.core.threat

enum class ThreatSource { LOCAL_HEURISTIC, CROWDSEC_CTI, GEMINI }

enum class HeuristicType {
    WEP_OPEN, EVIL_TWIN, ENCRYPTION_DOWNGRADE, KARMA_ATTACK,
    SSID_FLOODING, RSSI_ANOMALY, BSSID_FINGERPRINT
}

data class ThreatSignal(
    val confidence: Float,
    val source: ThreatSource,
    val reasons: List<String>,
    val heuristicType: HeuristicType?,
    val bssid: String,
    val detectedAt: Long = System.currentTimeMillis(),
    val schemaVersion: Int = 1
) {
    init {
        require(confidence in 0.0f..0.95f) {
            "Confidence must be 0.0–0.95, was $confidence"
        }
        require(reasons.size <= 3) {
            "Maximum 3 reasons, was ${reasons.size}"
        }
    }
}
```

- [ ] **Step 2: Create `SecurityType.kt`**

```kotlin
package com.wscanplus.core.threat

enum class SecurityType(val rank: Int) {
    WPA3(4), OWE(3), WPA2(3), WPA(2), WEP(1), OPEN(0)
}
```

- [ ] **Step 3: Create `ScanContext.kt`**

```kotlin
package com.wscanplus.core.threat

data class BssidProfile(
    val bssid: String,
    val firstSeenAt: Long,
    val lastSeenAt: Long,
    val ssids: Set<String>,
    val capabilitiesHistory: List<String>,
    val observationCount: Int,
    val ouiVendor: String?
)

enum class EnvironmentType { RESIDENTIAL, OFFICE, PUBLIC }

data class ScanInput(
    val bssid: String,
    val ssid: String,
    val isHidden: Boolean,
    val capabilities: String,
    val rssiDbm: Int,
    val frequencyMhz: Int,
    val channelWidth: Int,
    val timestamp: Long
)

data class ScanContext(
    val currentResults: List<ScanInput>,
    val knownProfiles: Map<String, BssidProfile>,
    val baselineNetworkCount: Int?,
    val baselineStdDev: Double?,
    val environmentType: EnvironmentType
)
```

- [ ] **Step 4: Write tests in `ThreatSignalTest.kt`**

```kotlin
package com.wscanplus.core.threat

import org.junit.Assert.*
import org.junit.Test

class ThreatSignalTest {
    @Test fun `confidence at lower bound`() {
        val signal = ThreatSignal(0.0f, ThreatSource.LOCAL_HEURISTIC, listOf("test"), HeuristicType.WEP_OPEN, "AA:BB:CC:DD:EE:FF")
        assertEquals(0.0f, signal.confidence, 0.001f)
    }

    @Test fun `confidence at upper bound`() {
        val signal = ThreatSignal(0.95f, ThreatSource.LOCAL_HEURISTIC, listOf("test"), HeuristicType.WEP_OPEN, "AA:BB:CC:DD:EE:FF")
        assertEquals(0.95f, signal.confidence, 0.001f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `confidence above 0_95 throws`() {
        ThreatSignal(0.96f, ThreatSource.LOCAL_HEURISTIC, listOf("test"), HeuristicType.WEP_OPEN, "AA:BB:CC:DD:EE:FF")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `confidence below 0 throws`() {
        ThreatSignal(-0.1f, ThreatSource.LOCAL_HEURISTIC, listOf("test"), HeuristicType.WEP_OPEN, "AA:BB:CC:DD:EE:FF")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `more than 3 reasons throws`() {
        ThreatSignal(0.5f, ThreatSource.LOCAL_HEURISTIC, listOf("a","b","c","d"), HeuristicType.WEP_OPEN, "AA:BB:CC:DD:EE:FF")
    }

    @Test fun `schemaVersion defaults to 1`() {
        val signal = ThreatSignal(0.5f, ThreatSource.LOCAL_HEURISTIC, listOf("test"), HeuristicType.WEP_OPEN, "AA:BB:CC:DD:EE:FF")
        assertEquals(1, signal.schemaVersion)
    }

    @Test fun `heuristicType nullable for CTI signals`() {
        val signal = ThreatSignal(0.5f, ThreatSource.CROWDSEC_CTI, listOf("test"), null, "AA:BB:CC:DD:EE:FF")
        assertNull(signal.heuristicType)
    }

    @Test fun `data class equality`() {
        val ts = System.currentTimeMillis()
        val a = ThreatSignal(0.5f, ThreatSource.LOCAL_HEURISTIC, listOf("r1"), HeuristicType.KARMA_ATTACK, "AA:BB:CC:DD:EE:FF", ts)
        val b = ThreatSignal(0.5f, ThreatSource.LOCAL_HEURISTIC, listOf("r1"), HeuristicType.KARMA_ATTACK, "AA:BB:CC:DD:EE:FF", ts)
        assertEquals(a, b)
    }

    // SecurityType rank verification — catches accidental reordering
    @Test fun `security type ranks are correct`() {
        assertEquals(4, SecurityType.WPA3.rank)
        assertEquals(3, SecurityType.OWE.rank)
        assertEquals(3, SecurityType.WPA2.rank)
        assertEquals(2, SecurityType.WPA.rank)
        assertEquals(1, SecurityType.WEP.rank)
        assertEquals(0, SecurityType.OPEN.rank)
    }
}
```

- [ ] **Step 5: Run tests**

Run: `cd android && ./gradlew :core:test --tests "com.wscanplus.core.threat.ThreatSignalTest" -q`
Expected: 9 tests PASS

- [ ] **Step 6: Run ktlint**

Run: `cd android && ./gradlew :core:ktlintCheck -q`
Expected: PASS (no violations)

- [ ] **Step 7: Commit, push, open PR**

```bash
git checkout -b claude/p2-threat-data-models
git add android/core/src/main/kotlin/com/wscanplus/core/threat/ThreatSignal.kt \
        android/core/src/main/kotlin/com/wscanplus/core/threat/SecurityType.kt \
        android/core/src/main/kotlin/com/wscanplus/core/threat/ScanContext.kt \
        android/core/src/test/kotlin/com/wscanplus/core/threat/ThreatSignalTest.kt
git commit -m "feat(p2): threat data models — ThreatSignal, SecurityType, ScanContext"
git push -u origin claude/p2-threat-data-models
gh pr create --draft --title "feat(p2): threat data models" --body "..."
```

Wait for CI + Copilot review. Merge with `gh pr merge --admin --squash`. Sync main.

---

## Task 2: SecurityType Parser + WEP/Open Heuristic (PR #2)

**Branch:** `claude/p2-wep-open-heuristic`

**Files:**
- Modify: `android/core/src/main/kotlin/com/wscanplus/core/threat/SecurityType.kt` (add parser)
- Create: `android/core/src/main/kotlin/com/wscanplus/core/threat/Heuristic.kt`
- Create: `android/core/src/main/kotlin/com/wscanplus/core/threat/WepOpenHeuristic.kt`
- Create: `android/core/src/test/kotlin/com/wscanplus/core/threat/SecurityTypeTest.kt`
- Create: `android/core/src/test/kotlin/com/wscanplus/core/threat/WepOpenHeuristicTest.kt`

- [ ] **Step 1: Add `parse()` to SecurityType.kt**

```kotlin
companion object {
    fun parse(capabilities: String): SecurityType {
        val caps = capabilities.uppercase()
        return when {
            caps.contains("SAE") -> WPA3
            caps.contains("OWE") -> OWE
            caps.contains("RSN") || caps.contains("WPA2") -> WPA2
            caps.contains("WPA") -> WPA
            caps.contains("WEP") -> WEP
            else -> OPEN
        }
    }
}
```

- [ ] **Step 2: Create `Heuristic.kt` interface**

```kotlin
package com.wscanplus.core.threat

interface Heuristic {
    val type: HeuristicType
    fun evaluate(input: ScanInput, context: ScanContext): ThreatSignal?
}
```

- [ ] **Step 3: Create `WepOpenHeuristic.kt`**

```kotlin
package com.wscanplus.core.threat

class WepOpenHeuristic : Heuristic {
    override val type = HeuristicType.WEP_OPEN

    override fun evaluate(input: ScanInput, context: ScanContext): ThreatSignal? {
        val security = SecurityType.parse(input.capabilities)
        val confidence = when (security) {
            SecurityType.OPEN -> 0.4f
            SecurityType.WEP -> 0.6f
            SecurityType.WPA -> 0.2f
            SecurityType.WPA2, SecurityType.OWE, SecurityType.WPA3 -> return null
        }
        return ThreatSignal(
            confidence = confidence,
            source = ThreatSource.LOCAL_HEURISTIC,
            reasons = listOf(reasonFor(security)),
            heuristicType = type,
            bssid = input.bssid
        )
    }

    private fun reasonFor(security: SecurityType): String = when (security) {
        SecurityType.OPEN -> "Open network — no encryption"
        SecurityType.WEP -> "WEP encryption — trivially crackable"
        SecurityType.WPA -> "WPA (original) — deprecated, known vulnerabilities"
        else -> ""
    }
}
```

- [ ] **Step 4: Write `SecurityTypeTest.kt`**

Tests for: SAE → WPA3, OWE → OWE, RSN → WPA2, WPA (without 2/3) → WPA, WEP → WEP, empty → OPEN, mixed capabilities string priority (SAE takes precedence), case insensitivity.

- [ ] **Step 5: Write `WepOpenHeuristicTest.kt`**

Tests for: OPEN returns 0.4, WEP returns 0.6, WPA returns 0.2, WPA2 returns null, WPA3 returns null, OWE returns null (Enhanced Open is NOT a threat), reason strings match.

- [ ] **Step 6: Run tests + ktlint**

Run: `cd android && ./gradlew :core:test -q && ./gradlew :core:ktlintCheck -q`

- [ ] **Step 7: Commit, push, PR, wait for CI, merge, sync**

---

## Task 3: Evil Twin + Encryption Downgrade (PR #3)

**Branch:** `claude/p2-evil-twin-downgrade`

**Files:**
- Create: `android/core/src/main/kotlin/com/wscanplus/core/threat/EvilTwinHeuristic.kt`
- Create: `android/core/src/main/kotlin/com/wscanplus/core/threat/EncryptionDowngradeHeuristic.kt`
- Create: `android/core/src/test/kotlin/com/wscanplus/core/threat/EvilTwinHeuristicTest.kt`
- Create: `android/core/src/test/kotlin/com/wscanplus/core/threat/EncryptionDowngradeHeuristicTest.kt`

- [ ] **Step 1: Implement `EvilTwinHeuristic`**

Constructor takes `ouiLookup: OuiLookup?` (nullable — null until PR 6).

5 sub-signals:
- OUI vendor mismatch: +0.3 (skipped when `ouiLookup == null`)
- Security capability mismatch: +0.35
- Same channel: +0.1
- New BSSID for established SSID: +0.2
- RSSI anomaly vs average: +0.15

Guest network filter: BSSIDs sharing first 5 octets = same hardware → skip.
Combined confidence: `1 - (1-a)(1-b)...` capped at 0.95.

- [ ] **Step 2: Implement `EncryptionDowngradeHeuristic`**

Compares `SecurityType.parse(input.capabilities)` rank vs highest rank in `BssidProfile.capabilitiesHistory`.
- Same-rank transitions (OWE↔WPA2): return null
- History minimum: `observationCount >= 3` AND history spans 2+ days (check `firstSeenAt` vs `lastSeenAt`)
- Confidence: WPA2/3→Open=0.85, WPA2/3→WEP=0.75, WPA→Open=0.5, other downgrades=0.3

- [ ] **Step 3: Write `EvilTwinHeuristicTest.kt`**

Tests: enterprise multi-AP same OUI NOT flagged, guest network (shared 5 octets) NOT flagged, capability mismatch alone → 0.35, OUI+capability mismatch → `1-(1-0.3)(1-0.35)` = 0.545, all 5 sub-signals combined, cap at 0.95, null OuiLookup skips OUI sub-signal.

- [ ] **Step 4: Write `EncryptionDowngradeHeuristicTest.kt`**

Tests: WPA2→Open=0.85, WPA3→WEP=0.75, WPA→Open=0.5, OWE→WPA2=null (same rank), insufficient history (<3 obs)=null, history <2 days=null, no profile in context=null.

- [ ] **Step 5: Run tests + ktlint, commit, push, PR, CI, merge, sync**

**LOC contingency:** If combined Evil Twin + Encryption Downgrade exceeds 200 LOC, split into two PRs: Evil Twin alone (PR 3a) and Encryption Downgrade alone (PR 3b). Subsequent PR numbers shift accordingly.

---

## Task 4: Karma + SSID Flooding (PR #4)

**Branch:** `claude/p2-karma-flooding`

**Files:**
- Create: `android/core/src/main/kotlin/com/wscanplus/core/threat/KarmaHeuristic.kt`
- Create: `android/core/src/main/kotlin/com/wscanplus/core/threat/SsidFloodingHeuristic.kt`
- Create: `android/core/src/test/kotlin/com/wscanplus/core/threat/KarmaHeuristicTest.kt`
- Create: `android/core/src/test/kotlin/com/wscanplus/core/threat/SsidFloodingHeuristicTest.kt`

- [ ] **Step 1: Implement `KarmaHeuristic`**

Group `context.currentResults` by BSSID. For each BSSID with multiple distinct non-hidden SSIDs:
- 3+ SSIDs → 0.45, 5+ → 0.75, 10+ → 0.95
- Bonus: same channel + tight RSSI (max-min <= 6dBm) increases confidence by 0.1 (capped)
- Exclude hidden: filter out entries where `isHidden == true`

- [ ] **Step 2: Implement `SsidFloodingHeuristic`**

Uses `context.baselineNetworkCount` and `context.baselineStdDev`.
- Return null if either is null (< MIN_BASELINE_SAMPLES)
- Z-score: `(currentResults.size - baseline) / stddev`
- Z >= 5.0 OR absolute > 200 → 0.90
- Z >= 3.5 → 0.70
- Z >= 2.5 → 0.50
- Below 2.5 → null

Note: emits one signal per scan (not per network), with bssid = "SCAN_LEVEL" sentinel.

- [ ] **Step 3: Write tests for both**

Karma tests: 2 SSIDs = null, 3 SSIDs = 0.45, 5 = 0.75, 10 = 0.95, hidden SSIDs excluded, tight RSSI bonus.
Flooding tests: null baseline = null, Z=2.4 = null, Z=2.5 = 0.50, Z=3.5 = 0.70, Z=5.0 = 0.90, absolute 201 = 0.90.

- [ ] **Step 4: Run tests + ktlint, commit, push, PR, CI, merge, sync**

---

## Task 5: RSSI Anomaly + BSSID Fingerprint (PR #5)

**Branch:** `claude/p2-rssi-fingerprint`

**Files:**
- Create: `android/core/src/main/kotlin/com/wscanplus/core/threat/RssiAnomalyHeuristic.kt`
- Create: `android/core/src/main/kotlin/com/wscanplus/core/threat/BssidFingerprintHeuristic.kt`
- Create: `android/core/src/test/kotlin/com/wscanplus/core/threat/RssiAnomalyHeuristicTest.kt`
- Create: `android/core/src/test/kotlin/com/wscanplus/core/threat/BssidFingerprintHeuristicTest.kt`

- [ ] **Step 1: Implement `RssiAnomalyHeuristic`**

Threshold by environment: RESIDENTIAL=-30, OFFICE=-25, PUBLIC=-20.
- New network (not in knownProfiles) + above threshold: >-20=0.80, >-25=0.60, >-30=0.45
- Known network + above threshold: >-20=0.30, >-25=0.15, >-30=null
- Below threshold: null

- [ ] **Step 2: Implement `BssidFingerprintHeuristic`**

Constructor takes `ouiLookup: OuiLookup?` (nullable until PR 6).
- Locally-administered MAC check: `(firstByte.toInt() and 0x02) != 0` → 0.25 base signal
- New BSSID for established SSID (>= 5 prior observations) + old BSSID disappeared within 30min → rotation signal
- OUI overlap (when ouiLookup available): same vendor = 0.15, different = 0.40
- Combined via `1 - (1-a)(1-b)` capped at 0.95

- [ ] **Step 3: Write tests for both**

RSSI tests: new network at -19dBm residential = 0.80, known at -19dBm = 0.30, -31dBm residential = null, office thresholds, public thresholds.
Fingerprint tests: locally-administered MAC detected, normal MAC not flagged, rotation within 30min, rotation outside 30min = lower, null OuiLookup skips vendor check, same vendor = lower confidence.

- [ ] **Step 4: Run tests + ktlint, commit, push, PR, CI, merge, sync**

---

## Task 6: OUI Asset Loader + Lookup (PR #6)

**Branch:** `claude/p2-oui-lookup`

**Files:**
- Create: `android/core/src/main/kotlin/com/wscanplus/core/threat/OuiLookup.kt`
- Create: `android/core/src/main/kotlin/com/wscanplus/core/threat/OuiAssetLoader.kt`
- Create: `android/core/src/main/assets/oui.csv` (bundled IEEE database — in :core so data stays with code)
- Create: `android/core/src/test/kotlin/com/wscanplus/core/threat/OuiLookupTest.kt`

- [ ] **Step 1: Create `OuiLookup.kt`** (pure Kotlin, no Android deps)

```kotlin
package com.wscanplus.core.threat

class OuiLookup(private val ouiMap: Map<String, String>) {

    private val suspiciousOuis = setOf(
        "B827EB", "DCA632", "E45F01", "2CCF67", "D83ADD",  // Raspberry Pi
        "240AC4", "30AEA4", "A4CF12", "CC50E3",            // Espressif
        "0CFA22",                                           // Flipper Devices
        "00C0CA",                                           // Alfa Network
    )

    fun lookup(bssid: String): String? {
        val prefix = bssid.uppercase().replace(":", "").take(6)
        return ouiMap[prefix]
    }

    fun isSuspiciousVendor(bssid: String): Boolean {
        val prefix = bssid.uppercase().replace(":", "").take(6)
        return prefix in suspiciousOuis
    }

    fun isLocallyAdministered(bssid: String): Boolean {
        val firstOctet = bssid.split(":").firstOrNull()
            ?.toIntOrNull(16) ?: return false
        return (firstOctet and 0x02) != 0
    }
}
```

- [ ] **Step 2: Create `OuiAssetLoader.kt`** (Android Context dependency isolated here)

```kotlin
package com.wscanplus.core.threat

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

object OuiAssetLoader {
    fun load(context: Context): OuiLookup {
        val map = mutableMapOf<String, String>()
        context.assets.open("oui.csv").use { stream ->
            BufferedReader(InputStreamReader(stream)).use { reader ->
                reader.lineSequence().drop(1).forEach { line ->
                    val parts = line.split(",", limit = 4)
                    if (parts.size >= 3) {
                        val oui = parts[1].trim().uppercase()
                        val vendor = parts[2].trim()
                        if (oui.length == 6) map[oui] = vendor
                    }
                }
            }
        }
        return OuiLookup(map)
    }
}
```

- [ ] **Step 3: Download and bundle `oui.csv`**

Download from `https://standards-oui.ieee.org/oui/oui.csv` and place at `android/core/src/main/assets/oui.csv`. Library module assets merge into APK automatically.

- [ ] **Step 4: Write `OuiLookupTest.kt`**

Tests with a small in-memory map (no asset loading needed for unit tests):
- Known OUI returns vendor name
- Unknown OUI returns null
- Suspicious vendor detected (Raspberry Pi OUI)
- Non-suspicious vendor returns false
- Locally-administered MAC detected (e.g., "x2:xx:xx:xx:xx:xx")
- Normal MAC returns false
- Case insensitivity (lowercase bssid works)
- Malformed BSSID doesn't crash

- [ ] **Step 5: Run tests + ktlint, commit, push, PR, CI, merge, sync**

---

## Task 7: Room + KSP Setup — Entities + TypeConverters (PR #7)

**Branch:** `claude/p2-room-entities`

**Files:**
- Modify: `android/build.gradle.kts` — add KSP + Room plugins
- Modify: `android/core/build.gradle.kts` — add KSP + Room plugins + deps
- Create: `android/core/src/main/kotlin/com/wscanplus/core/db/WscanDatabase.kt`
- Create: `android/core/src/main/kotlin/com/wscanplus/core/db/Converters.kt`
- Create: `android/core/src/main/kotlin/com/wscanplus/core/db/entity/ScanSessionEntity.kt`
- Create: `android/core/src/main/kotlin/com/wscanplus/core/db/entity/ScanResultEntity.kt`
- Create: `android/core/src/main/kotlin/com/wscanplus/core/db/entity/BssidFingerprintEntity.kt`
- Create: `android/core/src/main/kotlin/com/wscanplus/core/db/entity/ThreatSignalEntity.kt`
- Create: `android/core/src/main/kotlin/com/wscanplus/core/db/entity/CtiCacheEntity.kt`

- [ ] **Step 1: Add plugins to root `build.gradle.kts`**

```kotlin
id("com.google.devtools.ksp") version "2.2.10-2.0.2" apply false
id("androidx.room") version "2.8.4" apply false
```

- [ ] **Step 2: Add plugins + deps to `:core` `build.gradle.kts`**

Plugins block:
```kotlin
id("com.google.devtools.ksp")
id("androidx.room")
```

Room config:
```kotlin
room { schemaDirectory("$projectDir/schemas") }
```

Dependencies:
```kotlin
implementation("androidx.room:room-runtime:2.8.4")
implementation("androidx.room:room-ktx:2.8.4")
ksp("androidx.room:room-compiler:2.8.4")
```

- [ ] **Step 3: Create 5 entity files**

One file per entity, matching the schema in the design spec (see `project_phase2_research.md` for exact fields). Each entity includes `schemaVersion` where applicable. `CtiCacheEntity` PK = `cacheKey` string.

- [ ] **Step 4: Create `Converters.kt`**

`List<String>` ↔ JSON via `org.json.JSONArray` (Android built-in). Enums stored as raw strings.

- [ ] **Step 5: Create `WscanDatabase.kt`**

Abstract RoomDatabase. Version 1. `fallbackToDestructiveMigration()`. Singleton `getInstance(context)`. Schema export to `$projectDir/schemas`.

- [ ] **Step 6: Build to verify KSP/Room compiles**

Run: `cd android && ./gradlew :core:assembleDebug -q`
Expected: BUILD SUCCESSFUL (Room annotation processor generates code)

- [ ] **Step 7: Run ktlint, commit, push, PR, CI, merge, sync**

---

## Task 8: Room DAOs (PR #8)

**Branch:** `claude/p2-room-daos`

**Files:**
- Create: `android/core/src/main/kotlin/com/wscanplus/core/db/dao/ScanSessionDao.kt`
- Create: `android/core/src/main/kotlin/com/wscanplus/core/db/dao/ScanResultDao.kt`
- Create: `android/core/src/main/kotlin/com/wscanplus/core/db/dao/BssidFingerprintDao.kt`
- Create: `android/core/src/main/kotlin/com/wscanplus/core/db/dao/ThreatSignalDao.kt`
- Create: `android/core/src/main/kotlin/com/wscanplus/core/db/dao/CtiCacheDao.kt`
- Modify: `android/core/src/main/kotlin/com/wscanplus/core/db/WscanDatabase.kt` — add abstract DAO getters

- [ ] **Step 1: Create 5 DAO interfaces**

Each DAO uses `@Dao` annotation. Key methods per the spec:
- `ScanSessionDao`: insert, markCompleted, getRecent, getByTimeRange
- `ScanResultDao`: insertAll, getBySession, getByBssid
- `BssidFingerprintDao`: upsert, getByBssid, getRecentlyActive, getKnownBssidCount
- `ThreatSignalDao`: insert, insertAll, getByResult, getByBssid, getHighConfidence
- `CtiCacheDao`: upsert, getByCacheKey, getValidEntry, evictExpired

- [ ] **Step 2: Add abstract DAO getters to WscanDatabase**

```kotlin
abstract fun scanSessionDao(): ScanSessionDao
abstract fun scanResultDao(): ScanResultDao
abstract fun bssidFingerprintDao(): BssidFingerprintDao
abstract fun threatSignalDao(): ThreatSignalDao
abstract fun ctiCacheDao(): CtiCacheDao
```

- [ ] **Step 3: Build to verify**

Run: `cd android && ./gradlew :core:assembleDebug -q`

- [ ] **Step 4: Run ktlint, commit, push, PR, CI, merge, sync**

Note: Room in-memory DB integration tests require `androidTestImplementation("androidx.room:room-testing:2.8.4")` and run as instrumented tests. Defer these to a later maintenance PR if CI doesn't have an Android emulator. The unit tests for heuristic logic (PRs 2-5) are the primary test coverage for Phase 2.

---

## Task 9: PolicyGate + Engine + WatchdogService Wiring (PR #9)

**Branch:** `claude/p2-engine-wiring`

**Files:**
- Create: `android/core/src/main/kotlin/com/wscanplus/core/threat/PolicyGate.kt`
- Create: `android/core/src/main/kotlin/com/wscanplus/core/threat/HeuristicEngine.kt`
- Modify: `android/core/src/main/kotlin/com/wscanplus/core/scanner/WifiScanResult.kt` — add `toScanInput()`
- Modify: `android/app/src/main/kotlin/com/wscanplus/app/WatchdogService.kt` — wire engine
- Create: `android/core/src/test/kotlin/com/wscanplus/core/threat/PolicyGateTest.kt`
- Create: `android/core/src/test/kotlin/com/wscanplus/core/threat/HeuristicEngineTest.kt`

- [ ] **Step 1: Create `PolicyGate.kt`**

```kotlin
package com.wscanplus.core.threat

data class PolicyConfig(
    val minimumConfidence: Float = 0.3f,
)

class PolicyGate(private val config: PolicyConfig = PolicyConfig()) {
    fun filter(signals: List<ThreatSignal>): List<ThreatSignal> {
        return signals.filter { it.confidence >= config.minimumConfidence }
    }
}
```

Note: false-positive brakes (corp ASN + clean history) require CTI data and have been deferred to Phase 3. `PolicyGate` currently applies a confidence threshold filter only.

- [ ] **Step 2: Create `HeuristicEngine.kt`**

```kotlin
package com.wscanplus.core.threat

class HeuristicEngine(private val heuristics: List<Heuristic>) {
    fun analyze(context: ScanContext): List<ThreatSignal> {
        return context.currentResults.flatMap { input ->
            heuristics.mapNotNull { it.evaluate(input, context) }
        }
    }
}
```

- [ ] **Step 3: Add `toScanInput()` extension to `WifiScanResult.kt`**

```kotlin
fun WifiScanResult.toScanInput(): ScanInput = ScanInput(
    bssid = bssid,
    ssid = ssid,
    isHidden = ssid.isBlank(),
    capabilities = capabilities,
    rssiDbm = signalLevel,
    frequencyMhz = frequencyMhz,
    channelWidth = channelWidth,
    timestamp = timestamp
)
```

- [ ] **Step 4: Wire into `WatchdogService`**

Replace the stub lambda in `WatchdogService` with:
```kotlin
private val profiles = mutableMapOf<String, BssidProfile>()
private val ouiLookup: OuiLookup? = null  // injected after OUI loading
private val engine = HeuristicEngine(listOf(
    WepOpenHeuristic(),
    EvilTwinHeuristic(ouiLookup),
    EncryptionDowngradeHeuristic(),
    KarmaHeuristic(),
    SsidFloodingHeuristic(),
    RssiAnomalyHeuristic(),
    BssidFingerprintHeuristic(ouiLookup),
))
private val policyGate = PolicyGate()

// In the scanner callback:
ScannerChain(applicationContext) { results ->
    val inputs = results.map { it.toScanInput() }
    val context = ScanContext(
        currentResults = inputs,
        knownProfiles = profiles.toMap(),
        baselineNetworkCount = null,  // TODO: compute from Room after PR 7/8
        baselineStdDev = null,
        environmentType = EnvironmentType.RESIDENTIAL
    )
    val signals = engine.analyze(context)
    val filtered = policyGate.filter(signals)
    // TODO (Phase 4): persist to Room, surface to UI
    updateProfiles(results)
}
```

- [ ] **Step 5: Write `PolicyGateTest.kt`**

Tests: below-threshold filtered out, at-threshold passes, above-threshold passes, empty input returns empty, custom config respected.

- [ ] **Step 6: Write `HeuristicEngineTest.kt`**

Tests: empty heuristics list returns empty, single heuristic producing null returns empty, multiple heuristics produce correct signal count, engine passes context through.

Integration tests (engine + gate together): engine produces signals above and below threshold → gate filters correctly, engine produces empty → gate returns empty.

- [ ] **Step 7: Run all tests**

Run: `cd android && ./gradlew :core:test -q && ./gradlew :core:ktlintCheck -q && ./gradlew :app:ktlintCheck -q`

- [ ] **Step 8: Commit, push, PR, CI, merge, sync**

---

## Post-Phase 2 Checklist

After all 9 PRs merged:

- [ ] Run full quality gate: `./gradlew assembleDebug`, `:core:test`, `:core:ktlintCheck`, `:app:ktlintCheck`
- [ ] Update `docs/SESSION_STATE.md` — Phase 2 complete, PR log
- [ ] Update `docs/ROADMAP.md` — check off Phase 2 items
- [ ] Update `KNOWN_ISSUES.md` if any issues surfaced
- [ ] Design checkpoint issue (per AGENTS.md Section 6 — every 5 merged PRs)
