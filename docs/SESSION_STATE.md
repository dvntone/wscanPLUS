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
- Minimal CI only. Do NOT add CodeQL/SonarCloud/Semgrep/Detekt by default.
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
├── desktop/           # Electron app (Linux-first, ESM)
│   ├── src/
│   │   ├── main/      # Electron main process
│   │   └── renderer/  # UI — terminal/advanced + accessible modes
│   └── package.json   # "type": "module"
├── docs/              # All documentation
└── .github/
    └── workflows/     # CI only — no deploy/merge/trigger jobs
```

---

## Phase

**Phase 1 (Android Source)** — scaffold merged, first Kotlin sources next

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

## Phase 1 — In Progress

### Confirmed Android build stack (on main)

| Component | Version | Notes |
|-----------|---------|-------|
| AGP | 9.0.0 | 9.1.0 is alpha-only — do not use |
| Gradle | 9.3.1 | Wrapper SHA-256 pinned |
| compileSdk / targetSdk | 36 | — |
| minSdk | 24 | — |
| JDK | 17 | — |

### Kotlin configuration (resolved — PR #60)

AGP 9.0.0 ships with built-in Kotlin. No `org.jetbrains.kotlin.android` plugin needed. `jvmTarget` defaults to `compileOptions.targetCompatibility`. Modules with no Kotlin sources use `enableKotlin = false`. See KNOWN_ISSUES.md for full pattern.

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

### Firebase AI Logic / Gemini integration (Codex verified — 2026-03-16)

- **Android SDK:** `com.google.firebase:firebase-ai:16.0.0` via BOM `com.google.firebase:firebase-bom:35.5.0` (Firebase AI Logic SDK). Do NOT use `firebase-vertexai` (superseded) or `com.google.ai.client.generativeai` (deprecated).
- **Requires:** `com.google.gms:google-services:4.4.2` plugin + `google-services.json` at `android/app/google-services.json`
- **Root build.gradle.kts addition:** `id("com.google.gms.google-services") version "4.4.2" apply false`
- **App build.gradle.kts:** Add `id("com.google.gms.google-services")` to plugins block. No special `buildFeatures` needed.
- **Auth:** API key via `secrets-gradle-plugin:2.0.1` in `local.properties`. No WIF for Android runtime.
- **WIF (gemini_findings.md):** Valid for CI/CD → GCP server-side only. Filed for Phase 4+.
- **Google Maps SDK:** `com.google.android.gms:play-services-maps:18.1.0`

### WiFi scanning API (confirmed — 2026-03-16)

- `WifiManager.startScan()` deprecated API 28 — do not use for new scanner implementations.
- **Standard scanner pattern:** `BroadcastReceiver` for `WifiManager.SCAN_RESULTS_AVAILABLE_ACTION` + `wifiManager.getScanResults()`.
- `registerScanResultsCallback()` API 30+ only — guard with API level check.
- **Permissions (full set for API 24–36):** `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`, `ACCESS_FINE_LOCATION` (runtime), `NEARBY_WIFI_DEVICES` with `neverForLocation` flag (API 33+ only, declare in manifest).
- **USB adapter detection:** `android.hardware.usb.*` USB Host API (API 12+) — detect OTG adapters by vendor/product ID. No root required.

### Desktop test tooling (Codex verified — 2026-03-16)

- **Jest:** 29.7.0 → **30.3.0** (fixes CVE-2024-21538). `--experimental-vm-modules` no longer needed in Jest 30.
- **Updated test script:** `"test": "jest --runInBand --passWithNoTests"`
- **New file:** `jest.config.mjs` — `export default { testEnvironment: "node", transform: {}, extensionsToTreatAsEsm: [".js", ".mjs"] }`

### Phase 1 Remaining Actions (PR sequence)

1. **[PR — DEPENDENCY, do first — CVE blocker]** jest 29.7.0 → 30.3.0 + `jest.config.mjs` + test script update + Android test dep updates (core-ktx 1.18.0, test.ext:junit 1.3.0, espresso 3.6.1)
2. **[PR — FEATURE]** WatchdogService stub + scanner chain skeleton (USB > Standard; Root dev-opt-in stub)
3. **[PR — FEATURE]** AndroidManifest permissions (USE_BIOMETRIC, ACCESS_FINE_LOCATION, FOREGROUND_SERVICE, FOREGROUND_SERVICE_DATA_SYNC, INTERNET, NEARBY_WIFI_DEVICES)
4. **[PR — FEATURE]** Firebase AI Logic scaffold (`firebase-ai:16.0.0`, google-services plugin, `google-services.json` placeholder)
5. **[PR — FEATURE]** Google Maps scaffold

---

## Non-goals (for now)

- No offensive features by default (deauth injection, cracking, etc.)
- No Windows support until Linux path is stable
- No Flatpak/Snap packaging (sandbox blocks required capabilities)
- No tiered/paid licensing in initial build
