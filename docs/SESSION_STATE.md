# SESSION_STATE (source of truth)

Repo: https://github.com/dvntone/wscanplus
Name: wscan+ (WiFi Scan + Companion)
Audience: professional / advanced users (defensive detection & assessment)

---

## Core Architecture

- **Android** (Kotlin, API 24+): GPS companion, field sensor, tethering provider, distributed scanner node
- **Desktop** (Linux-first, Electron/Node): primary hub — scan aggregation, Kismet/BetterCap integration, PCAP analysis, AI heuristics
- **Web UI**: PWA dashboard served locally by desktop — not standalone for scanning

---

## Key Decisions (locked)

- Drop WiGLE service. Mapping via Google Maps (Android) + optional web map.
- Export formats: PCAP/PCAPNG (Wireshark-compatible) + JSON.
- Kismet: OPTIONAL integration. Primary near-term use: Android as remote GPS source.
- BetterCap: OPTIONAL integration. Reference: https://github.com/bettercap/caplets
- Minimal CI only. Do NOT add CodeQL/SonarCloud/Semgrep as GitHub code scanning integrations by default. Detekt removed from planning (2026-03-17); ktlint used instead.
- Secrets: NO tokens in repo. Android uses `local.properties` + secrets-gradle-plugin; desktop uses `.env` + dotenv.
- AI guardrails: 1 PR at a time, 1 issue per PR, tests-first, stop if CI red.
- **Node.js module system: ESM only.** All `package.json` files must have `"type": "module"`. No CommonJS.
- **Merge strategy: Squash only.** Merge commits and rebase are disabled.

---

## Tethering / Monitor Mode Model

- `airmon-ng` disables normal networking on the scan adapter.
- Android device tethers (USB or hotspot) to provide internet to desktop while adapter is in monitor mode.
- App must detect adapter count via `iw dev` and prompt user:
  - 1 adapter: "Connect a second adapter or tether from Android to maintain connectivity"
  - Android tethering detected: confirm which interface is scan vs. network

---

## ADB Communication Boundary

**Android → Desktop:** GPS position, scan metadata, threat events, tether status, device ID
**Desktop → Android:** commands, alerts, config updates, sync triggers

### Sync Modes (user-configurable, default is user-set)

| Mode | Description |
|------|-------------|
| Store + sync | Store on Android locally → sync when USB/LAN reconnected (default, offline-safe) |
| VPN tunnel | Push through reverse tunnel to desktop in real time |
| Standalone | Android runs independently; desktop is post-session review hub |

### Multi-Device

- Multiple Android devices can act as distributed field sensors simultaneously.
- Each device identified by unique device ID.
- Desktop aggregates and correlates data from all connected clients.
- Data model must support multi-device from day 1.

### ADB Connection Priority

1. USB ADB (most reliable, used when tethered)
2. Wireless ADB (same LAN)
3. User-configured remote sync

---

## Folder Structure

```
wscanplus/
├── android/           # Kotlin companion app (Gradle)
│   ├── app/           # Application module
│   ├── core/          # Library module
│   └── gradle/        # Wrapper + daemon config
├── desktop/           # Electron app (Linux-first, ESM) — flat layout post-#200
│   ├── main.js        # Electron main process
│   ├── preload.js     # Context bridge (window.wscan + window.wscanDesktop)
│   ├── renderer.mjs   # Renderer UI
│   ├── scanner.mjs    # iw-based WiFi scan loop
│   ├── detector.mjs   # Threat scoring
│   ├── store.mjs      # EventEmitter state store
│   ├── companionServer.mjs  # WebSocket companion server (token auth)
│   ├── adbPreflight.mjs     # ADB device readiness checks
│   ├── adb/           # AdbTransport (WatchdogService tcp:9000)
│   ├── companion.js   # Companion UI shell
│   ├── sessionWorkbench.js  # Session artifact import
│   └── package.json   # "type": "module"
├── docs/              # All documentation
└── .github/
    └── workflows/     # CI only — no deploy/merge/trigger jobs
```

> Note: `dvntone/wscanplus_desktop` was a separate companion repo that has been consolidated into `desktop/` via PR #200 and archived. It is no longer a live dependency.

---

## Phase

**Phase 1 + Phase 2 — complete.** Phase 3 planning next.

**Phase 2 (Local Threat Intelligence) complete as of 2026-03-20:**
- 7 WiFi threat heuristics live (WEP/Open, Evil Twin, Encryption Downgrade, Karma, SSID Flooding, RSSI Anomaly, BSSID Fingerprint)
- HeuristicEngine + PolicyGate wired into WatchdogService
- Room DB (5 entities, 5 DAOs) + OUI asset loader
- ~100+ unit tests passing

See [docs/ROADMAP.md](/docs/ROADMAP.md) for the full phased plan.

---

## 2026-04-04 Handoff Snapshot

This section is the fast re-entry point for the next session.

### Remote repo state (2026-04-04)

- `main` is current — latest merge: PR `#200` (desktop consolidation)
- No open PRs
- No open blocking issues
- `dvntone/wscanplus_desktop` archived — desktop runtime now lives in `desktop/` of this monorepo
- `dvntone/wscanplus-deprecrated-` archived
- `dvntone/flipp3d` archived

### Completed phases (as of 2026-04-04)

- **Phase 0–2**: Complete (Android scanner, heuristics, Room DB, OUI)
- **Phase 3**: Complete (consent framework, CrowdSec CTI, Firebase, CTI cache, quota guardrails, SQLCipher, scan history)
- **Phase 4**: Complete (GeminiThreatAnalyzer, scan history timeline, ADB transport, companion shell, JSON export, ESM preload bridge)
- **Phase 5 (partial)**: CapabilityProbe layer merged (PR #198). Desktop consolidated (PR #200).

### Phase 5 remaining work

1. **Transport hello update** — include `DeviceCapabilityManifest` JSON in WatchdogService hello message to desktop
2. **Spatial WiFi floor tracking** — barometer-based relative floor detection (handoff spec in `/mnt/c/Users/Devia/AppData/Local/Temp/wscan_pull/handoff.md`)
3. **darklotusLABS web UI** — Sentinel Prism theme, NYX assistant, Vite build for app.darklotuslabs.com

### Active repos

| Repo | Status |
|------|--------|
| `dvntone/wscanplus` | Active — canonical monorepo |
| `dvntone/wscanplus_webui` | Active — darklotuslabs.com placeholder (sweep-tool-v2.jsx) |
| `dvntone/MetaRadar-Clone` | Reference only |

---

## 2026-03-20 Handoff Snapshot

This section is the fast re-entry point for the next Claude/Copilot session.

### Remote repo state

- `main` is current and PR `#148` is merged
- `KNOWN_ISSUES.md` records the Phase 2 completion snapshot and triage follow-ups added on 2026-03-20
- Issue `#116` (app-side logging) was closed after merge
- Issues `#143` and `#145` were resolved and closed on 2026-03-20 after PR #144 and PR #146 merged
- Issue `#10` was resolved and closed on 2026-03-20 after PR #148 merged
- Open tracked work at repo level is now:
  - `#121` — device-testing docs/session prep cleanup follow-up
  - `#122` — missing visible app-side adb logs on Revvl Android 15
  - `#124` — coarse-only launch succeeds but scan retrieval still fails
  - `#125` — backgrounded / keyguard-visible app loses effective `getScanResults()` access
  - `#9` — Google Maps threat heatmap + scan history map

### Verification status

- Latest local Android verification completed successfully on 2026-03-20:
  - `./gradlew :core:test`
  - `./gradlew :core:ktlintCheck :app:ktlintCheck`
- Additional local Android verification completed successfully on 2026-03-20 during follow-up fixes:
  - `cmd /c gradlew.bat :core:test`
  - `cmd /c gradlew.bat :core:ktlintCheck :app:ktlintCheck`
- `.env` / `local.properties` remain untracked
- Android app versioning has now moved off the scaffold placeholder:
  - `versionCode = 2`
  - `versionName = "0.1.0"`
- Issue `#10` verification completed successfully before PR `#148` merged on 2026-03-20:
  - `cmd /c gradlew.bat :core:test :app:testDebugUnitTest :app:assembleDebug --rerun-tasks`
  - `cmd /c gradlew.bat :core:ktlintCheck :app:ktlintCheck --rerun-tasks`
- Current `#10` implementation direction is documented in [57_issue_10_kismet_web_gps_delivery_2026-03-20.md](/docs/research/codex/57_issue_10_kismet_web_gps_delivery_2026-03-20.md)

### Cross-repo dependency

- `dvntone/wscanplus_desktop` was consolidated into `desktop/` of this monorepo via PR #200 on 2026-04-04 and is now archived.
- All desktop development happens in `desktop/` of this repo only.

### Device-testing docs standard

- Device-testing material is now split by purpose:
  - `docs/testing/shared/` for repeatable procedures
  - `docs/testing/devices/<device>/` for device-specific summaries
  - `docs/research/` for durable non-device research notes
- Raw captures and scratch reference dumps belong under git-ignored local paths, not top-level `docs/`
- Current active Android target is `Revvl Tab 2` (Android 15)
- Previous archived device-testing baseline is `moto g play - 2024`
- Latest strict-environment target is `Pixel 10 Pro XL` on the current beta track
  - First Revvl baseline result is documented in `docs/testing/devices/revvl-tab-2/2026-03-20-baseline-smoke-test.md`
  - Pixel beta / Advanced Protection install baseline is documented in `docs/testing/devices/pixel-10-pro-xl/README.md`
- Runtime discrepancy from that baseline is tracked in issue `#122` (missing visible app-side adb logs on Revvl Android 15)
- Android 15 permission strategy note is documented in `docs/research/android_wifi_location_strategy.md`
- Current technical stance:
  - `ACCESS_FINE_LOCATION` remains the known-good requirement for scan retrieval on targetSdk 36
  - coarse-only is not part of the app's main intended operating mode and must not be treated as normal scan capability
  - any future coarse-only fallback should be an explicit optional degraded mode with clear user notice, not the default scanner path
  - `ACCESS_BACKGROUND_LOCATION` is the intended field-mode requirement, but current Android 15/OEM behavior still needs re-verification before treating it as sufficient everywhere
- Additional Revvl findings now tracked:
  - `#124` - coarse-only launch succeeds but scan retrieval still fails
  - `#125` - backgrounded / keyguard-visible app loses effective `getScanResults()` access while shell scans still work
- Cross-device validation status:
  - baseline: Revvl Tab 2 / Android 15 and moto g play - 2024 / Android 14 both reproduced the background / keyguard scan-access failure
  - issue `#126` fix is now merged: `ACCESS_BACKGROUND_LOCATION` + `foregroundServiceType="location|dataSync"`
  - moto g play - 2024 / Android 14 showed improved locked-screen recovery with the current fix when device location mode was enabled, but that result must not be generalized to other OEM paths
  - Revvl Tab 2 / Android 15 received a non-secure-lockscreen re-test on current `main` during this session:
    - foreground launch works on build `0.1.0` / `versionCode=2`
    - `WatchdogService` starts and remains foreground after `HOME`
    - `WatchdogService` also remained foreground after screen-off in the non-secure-lockscreen scenario
    - the previous Revvl `getScanResults not allowed ... has no location permission` signature was not reproduced in that non-secure pass
    - coarse-only re-test on current `main` no longer reaches scanner startup; `MainActivity` blocks before `WatchdogService` when `ACCESS_FINE_LOCATION` is absent
    - secure-lockscreen behavior on Revvl still needs its own current-main validation
  - Pixel 10 Pro XL beta-track baseline: trusted-host `adb install -r` (install/update) succeeded with Advanced Protection still enabled, including a repeat install with the lockscreen showing
  - Pixel 10 Pro XL full matrix is documented in `docs/testing/devices/pixel-10-pro-xl/2026-03-20-full-adb-matrix.md`, but its results should not be generalized onto Revvl-specific behavior
  - coarse-only remains degraded/incomplete and should be treated as an explicit product decision area unless re-verified per device path
  - future desktop implementation should reuse the host-side adb checks captured in `docs/testing/shared/50_desktop_adb_handoff.md`

### Local workspace caution

- The local checkout may still contain intentional local-only files such as `.vscode/extensions.json`
- Do not stage or revert local editor settings blindly in future sessions

### Recommended next work

1. Issue `#9` — add Google Maps threat heatmap and GPS-tagged scan history map on top of the stored GPS fields implemented in PR `#148` (which closed issue `#10`)
2. Resolve the remaining Android runtime issues already tracked:
   - `#125` re-test secure-lockscreen / stronger background cases on Revvl Android 15, because non-secure HOME and screen-off did not reproduce the earlier failure on current `main`
   - `#124` align issue/docs state with current behavior: coarse-only is now blocked before service startup on current `main`
   - `#122` app-side adb log visibility on Revvl Android 15
3. Carry the documented adb install / permission / state checks into the later desktop companion implementation
4. Read [docs/research/codex/54_review_triage_2026-03-20.md](/docs/research/codex/54_review_triage_2026-03-20.md) before changing scanner behavior, Windows wrapper behavior, or cross-repo hardening assumptions
5. Read [docs/research/codex/55_open_issue_priority_2026-03-20.md](/docs/research/codex/55_open_issue_priority_2026-03-20.md) for the current research-backed priority order and source links
6. Read [docs/research/codex/56_map_provider_options_2026-03-20.md](/docs/research/codex/56_map_provider_options_2026-03-20.md) before proposing any replacement or fallback for the locked Google Maps integration
7. Read [docs/research/codex/57_issue_10_kismet_web_gps_delivery_2026-03-20.md](/docs/research/codex/57_issue_10_kismet_web_gps_delivery_2026-03-20.md) before changing the GPS/Kismet path or comparing the current branch against the earlier plan

---

## Phase 0 — Complete ✅

All PRs merged: #40, #43, #45, #47, #49, #51, #53, #55, #57

1. ~~Add guardrails: AGENTS.md + .github/copilot-instructions.md~~ ✅
2. ~~Add docs: INDEX, ROADMAP, DEPENDENCIES, SESSION_STATE~~ ✅
3. ~~Add secrets scaffolding: .env.example + secrets.defaults.properties + .gitignore~~ ✅
4. ~~Fix and complete CI: timeouts + Android unit test + blocking secret scan~~ ✅
5. ~~Configure GitHub repo secrets~~ ✅ — 7 secrets configured 2026-03-15
6. ~~Android scaffold (Gradle project foundation)~~ ✅ — PR #57 merged 2026-03-15

---

## Phase 1 — Complete

### Confirmed Android build stack (on main)

| Component | Version | Notes |
|-----------|---------|-------|
| AGP | 9.1.0 | Stable since 2026-03-03. |
| Gradle | 9.4.0 | Wrapper SHA-256 pinned — meets AGP 9.1.0 minimum (9.3.1+) |
| compileSdk / targetSdk | 36 | — |
| minSdk | 24 | — |
| JDK | 17 | — |

### Kotlin configuration (resolved — PR #60)

AGP 9.x ships with built-in Kotlin. No `org.jetbrains.kotlin.android` plugin needed. `jvmTarget` defaults to `compileOptions.targetCompatibility`. Modules with no Kotlin sources use `enableKotlin = false`. See KNOWN_ISSUES.md for full pattern.

`CoreModule.kt` stub is in `core/` — Kotlin compilation verified end-to-end.

### Scanner chain (revised — 2026-03-16)

| Scanner | Status | Reason |
|---------|--------|--------|
| Nexmon | **Removed** | Broadcom-only; incompatible with primary test devices (OnePlus 10T = Snapdragon, Pixel 10 Pro XL = Tensor). Requires firmware flashing. Not viable for mainstream distribution. |
| Shizuku | **Removed** | Already dropped (REFERENCES.md). Research confirmed: adds zero scanning capability. No bypass of scan throttling, no monitor mode. |
| USB | **Keep** | External USB OTG adapter — reliable enhanced scanning for advanced users. |
| Root | **Dev opt-in only** | Works only on custom-kernel devices. Must be gated behind explicit flag; never a silent chain fallback. |
| Standard | **Keep — guaranteed baseline** | WifiManager — works on all devices, all API levels. |

**Effective chain: USB > Standard. Root is an explicit dev-mode stub only.**

### ADB transport (confirmed — 2026-03-16)

- **Android side:** WatchdogService foreground Service. `foregroundServiceType="dataSync"` (NOT `connectedDevice`). Listens on `localhost:9000` via `java.net.ServerSocket` (no extra dependency needed).
- **Manifest permissions for WatchdogService:** `FOREGROUND_SERVICE` (API 31+) + `FOREGROUND_SERVICE_DATA_SYNC` (API 34+) + `INTERNET`
- **Android 15 note:** `dataSync` services have a 6-hour max runtime — WatchdogService must handle clean restart.
- **Desktop ADB library:** **Deferred to Phase 3.** `@u4/adbkit` v5.1.7 confirmed CJS-only — no ESM exports. Incompatible with ESM-only rule. Tango ADB to be evaluated for native ESM at Phase 3 before any library is added.
- **Multi-device:** Serial as primary key in all data structures — architecture locked, implementation Phase 3.
- **Wireless ADB:** Same forwarding post-`adb pair` / `adb connect`. Developer options must be enabled by user — no programmatic enablement.
- **ya-webadb:** Skip — reserved for future web dashboard only.
- **LocalSocket:** NOT used for desktop↔Android comms. Standard TCP `ServerSocket` + ADB port forwarding is correct.

### Firebase AI Logic / Gemini integration (re-verified Codex — 2026-03-16)

- **Android SDK:** `com.google.firebase:firebase-ai` via BOM `com.google.firebase:firebase-bom:34.10.0` (Firebase AI Logic SDK). No explicit version on `firebase-ai` when using BOM. Standalone pin is `firebase-ai:17.10.0`.
- **Previous lock (35.5.0 BOM / 16.0.0 artifact) was incorrect** — BOM 35.x does not exist; 34.10.0 is current stable (released 2026-02-26). `firebase-ai` 16.x is superseded; stable line is 17.x.
- **Breaking changes 16.x → 17.x** (all pre-code — no migration needed since no Firebase code written yet): minSdk bumped to 23 (project minSdk 24 — compatible); `generateContent()`/`countTokens()` require ≥1 argument; grounding metadata fields are now non-optional.
- **Do NOT use:** `firebase-vertexai` (superseded), `com.google.ai.client.generativeai` (deprecated).
- **Requires:** `com.google.gms:google-services:4.4.4` plugin + `google-services.json` at `android/app/google-services.json`
- **Root build.gradle.kts addition:** `id("com.google.gms.google-services") version "4.4.4" apply false`
- **App build.gradle.kts:** Add `id("com.google.gms.google-services")` to plugins block. No special `buildFeatures` needed.
- **Dependency block (app + core):**
  ```kotlin
  implementation(platform("com.google.firebase:firebase-bom:34.10.0"))
  implementation("com.google.firebase:firebase-ai")
  ```
- **Auth:** API key via `secrets-gradle-plugin:2.0.1` in `local.properties`. No WIF for Android runtime.
- **WIF (gemini_findings.md):** Valid for CI/CD → GCP server-side only. Filed for Phase 4+.
- **Google Maps SDK:** `com.google.android.gms:play-services-maps:20.0.0`

### WiFi scanning API (confirmed — 2026-03-16)

- `WifiManager.startScan()` deprecated API 28 — do not use for new scanner implementations.
- **Standard scanner pattern:** `BroadcastReceiver` for `WifiManager.SCAN_RESULTS_AVAILABLE_ACTION` + `wifiManager.getScanResults()`.
- `registerScanResultsCallback()` API 30+ only — guard with API level check.
- **Permissions (full set for API 24–36):** `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`, `ACCESS_FINE_LOCATION` + `ACCESS_COARSE_LOCATION` (both required at runtime — Android 12+ mandates both declared; user may grant only COARSE and app must handle that gracefully), `NEARBY_WIFI_DEVICES` with `neverForLocation` flag (API 33+ only, declare in manifest).
- **USB adapter detection:** `android.hardware.usb.*` USB Host API (API 12+) — detect OTG adapters by vendor/product ID. No root required.

### Desktop test tooling (Codex verified — 2026-03-16)

- **Jest:** 29.7.0 → **30.3.0** (fixes CVE-2024-21538). `--experimental-vm-modules` no longer needed in Jest 30.
- **Updated test script:** `"test": "jest --runInBand --passWithNoTests"`
- **New file:** `jest.config.mjs` — `export default { testEnvironment: "node", transform: {} }` — `extensionsToTreatAsEsm` is NOT needed for `.js`/`.mjs` with `"type": "module"` in Jest 30.

### Phase 1 PR Log

All merged to main through PR #77 (last feature commit: `11e4a45`):

1. ~~**[PR #62]** jest 29.7.0 → 30.3.0 (CVE-2024-21538) + Android test dep updates~~ ✅ e95a880
2. ~~**[PR #63]** AGP 9.0.0 → 9.1.0~~ ✅ e7e9fbe
3. ~~**[PR #64]** Dependency audit + doc alignment~~ ✅ 3ebd218
4. ~~**[PR #65]** WatchdogService stub + scanner chain skeleton~~ ✅ 4ea62e2
5. ~~**[PR #66]** AndroidManifest permissions (full set)~~ ✅ 4fd20fc
6. ~~**[PR #67]** Firebase AI Logic scaffold (firebase-bom:34.10.0 + firebase-ai)~~ ✅ 94c5c59
7. ~~**[PR #68]** Google Maps scaffold (play-services-maps:20.0.0, GOOGLE_MAPS_API_KEY)~~ ✅ 1218a1e
8. ~~**[PR #69]** Lint fix — ACCESS_COARSE_LOCATION + @RequiresPermission annotations~~ ✅ 10f753f
9. ~~**[PR #70]** Docs — post-quality-gate SESSION_STATE + KNOWN_ISSUES~~ ✅ 3d54c1f
10. ~~**[PR #71]** deps — Gradle 9.3.1 → 9.4.0 + espresso-core 3.6.1 → 3.7.0~~ ✅ 9ea356d
11. ~~**[PR #72]** WatchdogService `startForeground()` + notification channel~~ ✅ 89d4f1a
12. ~~**[PR #73]** Docs — post-PR #72 SESSION_STATE + KNOWN_ISSUES update~~ ✅ b512aea
13. ~~**[PR #74]** Stub MainActivity — permission flow + WatchdogService start~~ ✅ df7b7c0
14. ~~**[PR #75]** StandardScanner implementation + WifiScanResult data model~~ ✅ 00ec021
15. ~~**[PR #76]** StandardScanner Copilot fixes — executor threading, executor leak, CHANGE_WIFI_STATE~~ ✅ 5f03771
16. ~~**[PR #77]** Wire scanner results into WatchdogService + WifiScanResult Phase 1 fields~~ ✅ 11e4a45

### Quality Gate (2026-03-16)

| Check | Result |
|-------|--------|
| `./gradlew assembleDebug` | ✅ BUILD SUCCESSFUL (Gradle 9.4.0) |
| `npm test` (desktop) | ✅ Pass |
| `./gradlew lint` | ✅ 0 errors, 2 warnings at the time of the 2026-03-16 quality gate; those icon / data-extraction items were completed later in Phase 1 |
| `./gradlew :core:ktlintCheck :app:ktlintCheck` | ✅ Added (issue #83) |

### WatchdogService notification (PR #72 — confirmed pattern)

- `createNotificationChannel()` in `onCreate()` — `IMPORTANCE_LOW`, API 26+ guard
- `ServiceCompat.startForeground()` at top of `onStartCommand()` with `FOREGROUND_SERVICE_TYPE_DATA_SYNC`
- `@SuppressLint("InlinedApi")` — constant is API 29, safe because `ServiceCompat` guards internally
- Placeholder icon `android.R.drawable.ic_menu_search` — replace in Phase 2

### Threat Pipeline Architecture (locked 2026-03-17)

Three-layer design — rated 8.5/10 by Codex review:

| Layer | What | Cost | When |
|-------|------|------|------|
| 1 — Local heuristics | WEP detection, evil twin signals, unknown BSSID patterns | Free, unlimited, on-device | Every scan |
| 2 — CrowdSec CTI API | IP reputation, classification (VPN/proxy/Tor/botnet), behavior signals | 30 req/week free / 100 req/week premium; cached locally (Room on Android; sqlite/file on Desktop) | Only IPs passing Layer 1 suspicion threshold |
| 3 — Gemini (firebase-ai) | Natural language threat assessment on combined Layer 1 + 2 signal | API cost; cache results | On-demand or threshold trigger |

**CTI API endpoints:**
- `GET /v2/smoke/{ip}` — per-IP stable reputation lookup (smoke dataset, 48h TTL). Primary lookup path.
- `GET /v2/fire` — bulk feed of recently active aggressors (fire dataset, 6h TTL). Returns a list, not per-IP. Cache stores presence in feed per IP.

Auth: `x-api-key` header. Android: OkHttp. Electron: `fetch()`. nodejs-bouncer: ESM-first but remediation-only — not needed for CTI lookups.

**Six required refinements (Codex — 2026-03-17):**

1. **Confidence propagation** — each layer passes a numeric confidence + provenance into the next so Gemini weighs signals rather than treating them equally
2. **Differentiated cache TTLs** — CTI "fire" (volatile, shorter TTL) vs "smoke" (stable reputation, longer TTL e.g. 48h)
3. **False-positive brakes** — benign override heuristic: known corp ASN + clean CTI + stable RSSI → suppress escalation
4. **Degraded-mode behavior** — define behavior when CTI unavailable: serve cache-only, or skip to Gemini with "CTI missing" flag in payload
5. **Explainability payload** — save top 3 reasons per layer for UI display and audit logs — makes results defensible
6. **Quota budget guardrails** — hard caps per time window for both CTI and Gemini to prevent runaway bursts on noisy environments

**CTI cache prerequisite:**
- **Android:** Room DB cache where the **primary key** is `cacheKey` with format `"$ip:${dataset.name}"` (one row per IP per dataset). Optionally add a **non-unique** index on `(ip, dataset)` for query performance. smoke TTL: 48h; fire TTL: 6h.
- **Desktop (Electron):** sqlite or file-based cache with the same logical keying: primary key column `cacheKey` using `"$ip:${dataset.name}"`, with any `(ip, dataset)` index non-unique and used only for performance — Room is Android-only.

Both platforms must implement their cache layer before making any CTI API calls.

### AI Layer Threat Data Model (Phase 4 — locked shape, defined ahead of time)

> Corresponds to **Phase 4 — AI Layer** in ROADMAP.md. Defined during Phase 1 so the shape is locked before implementation begins.

**Three primary additions** (ThreatSignal, CtiCacheEntry, CTI_MISSING_FLAG) plus **two supporting enum types** (ThreatSource, CtiDataset). All five belong to Phase 4 (AI Layer) per ROADMAP. The shapes are locked now so DB schema can be reserved from Phase 2 onward without breaking changes:

```
// Supporting enum — signal layer origin
enum class ThreatSource { LOCAL_HEURISTIC, CROWDSEC_CTI, GEMINI }

// Supporting enum — CTI dataset. Also determines cache TTL.
// Cache key for CtiCacheEntry is the string "$ip:${dataset.name}" — one row per ip+dataset pair.
enum class CtiDataset { SMOKE, FIRE }

// Primary addition 1 — per-layer signal wrapper
data class ThreatSignal(
    val confidence: Float,        // 0.0–1.0 confidence
    val source: ThreatSource,     // layer origin
    val reasons: List<String>,    // top 3 human-readable reason strings (UI + audit log)
)

// Primary addition 2 — CTI cache entry
// Android: Room @Entity, @PrimaryKey val cacheKey: String = "$ip:${dataset.name}"
// Desktop: sqlite row with same string primary key
// Unique by design: ip+dataset string key ensures one row per IP per dataset (SMOKE or FIRE)
// TTL: SMOKE → 48h, FIRE → 6h
data class CtiCacheEntry(
    val cacheKey: String,         // "$ip:${dataset.name}" — primary key
    val ip: String,
    val dataset: CtiDataset,      // SMOKE (per-IP lookup) | FIRE (presence in bulk feed)
    val responseJson: String,
    val cachedAt: Long,           // epoch ms
)

// Primary addition 3 — degraded-mode flag injected into Gemini prompt context
// when CTI is unavailable (offline / quota exhausted / cache miss + expired)
const val CTI_MISSING_FLAG = "cti_unavailable"
```

### Phase 1 Closeout

1. ~~**ktlint CI addition**~~ ✅ — `org.jlleitschuh.gradle.ktlint:14.2.0`, zero-config. CI: `./gradlew :core:ktlintCheck :app:ktlintCheck`. Detekt removed from planning (2026-03-17).
2. **Phase reassessment docs** — ROADMAP.md update with revised phase structure (2026-03-17 reassessment)
3. ~~**Phase 1 remaining**~~ ✅ app icon, `dataExtractionRules`, settings deep link on permission denial, and Android 15 timeout handling were completed in follow-on work through PR `#137` and PR `#135`.
4. **eslint advisory dep PR** — eslint 9.x → 10.0.3 (no CVE, deferred)
5. ~~**First unit tests**~~ ✅ `:core` module pure-logic test baseline already present on `main`

### StandardScanner (current main pattern)

- API 30+: `registerScanResultsCallback()` with `Executors.newSingleThreadExecutor()`. Fields assigned after registration; executor shut down on registration failure.
- API 24–29: `BroadcastReceiver` + `getScanResults()`. Results dispatched via same executor (`executor.execute {}`).
- Both paths deliver `onResults()` off the main thread — consistent threading.
- Legacy API 24–29 path now requests `startScan()` and logs when the request is rejected.
- `@RequiresPermission`: `ACCESS_WIFI_STATE` + `ACCESS_FINE_LOCATION` + `CHANGE_WIFI_STATE` on both `StandardScanner.start()` and `ScannerChain.start()`.
- Follow-up triage note: [docs/research/codex/54_review_triage_2026-03-20.md](/docs/research/codex/54_review_triage_2026-03-20.md) records the review history that led to the March 20 scanner and wrapper fixes.

---

## Non-goals (for now)

- No offensive features by default (deauth injection, cracking, etc.)
- No Windows support until Linux path is stable
- No Flatpak/Snap packaging (sandbox blocks required capabilities)
- No tiered/paid licensing in initial build
