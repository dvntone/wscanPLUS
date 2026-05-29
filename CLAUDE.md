# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Read First

Before any session work, read these files in order:

1. `docs/SESSION_STATE.md` — **authoritative source of current project state**. Use this, not `PROJECT_OVERVIEW.md` or `ROADMAP.md`, for implementation decisions.
2. `AGENTS.md` — agent rules summary
3. `KNOWN_ISSUES.md` — active bugs and deferred items
4. `docs/ROADMAP.md` — phased plan (for context only, not sprint planning)
5. `.claude/HARDWARE_MANIFEST.md` — confirmed hardware inventory, pinouts, and wiring. Read before any hardware/ESP32/sensor work. *(local-only — gitignored; not present in fresh clones)*

## Commands

### Android (run from `android/`)

```bash
./gradlew :core:test :app:test          # Unit tests (CI gate)
./gradlew :core:ktlintCheck :app:ktlintCheck  # Kotlin lint (CI gate)
./gradlew assembleDebug                  # Debug build
./gradlew :core:test                     # Core module tests only
./gradlew :app:testDebugUnitTest         # App module tests only
```

### Desktop (run from `desktop/`)

```bash
npm test        # Jest (adb/) + node:test (all *.test.js files) — CI gate
npm run lint    # ESLint, max-warnings=0 — CI gate
npm start       # Launch Electron app (Linux only; requires display)
```

### Single test (Android)

```bash
./gradlew :core:test --tests "com.wscanplus.core.YourTestClass"
```

### Single test (Desktop)

```bash
node --test desktop/scanner.test.js
```

### Secrets setup (local only — never commit)

- **Android**: create `android/local.properties` with `CROWDSEC_CTI_API_KEY=`; place `google-services.json` at `android/app/google-services.json`
- **Desktop**: copy `.env.example` to `desktop/.env` and fill in keys

## Architecture

### Three-component system

| Component | Stack | Status |
|-----------|-------|--------|
| Android companion app | Kotlin, API 24+, Gradle | Phases 0–4 complete |
| Desktop hub | Electron, Linux-first, ESM | Phase 5 partial |
| Web UI (PWA) | Not yet built — darklotusLABS, Vite | Planned |

### Android — two Gradle modules

**`core/`** — pure library, no Firebase, no Android UI:
- `scanner/` — `ScannerChain` (USB > Standard; root is dev-only), `StandardScanner`, `WifiScanResult`
- `threat/` — `HeuristicEngine` + 7 heuristics (`WepOpen`, `EvilTwin`, `EncryptionDowngrade`, `Karma`, `SsidFlooding`, `RssiAnomaly`, `BssidFingerprint`), `PolicyGate` (confidence threshold 0.3), `OuiLookup`
- `db/` — `WscanDatabase` (Room + SQLCipher AES-256), 6 entities and DAOs, `RetentionManager` (30-day purge)

**`app/`** — application module, depends on `:core`:
- `WatchdogService` — foreground service (`foregroundServiceType="location|dataSync"`), runs `ScannerChain`, hosts `ServerSocket` on `localhost:9000` for ADB transport, wires all layers
- `cti/` — `CrowdSecCtiClient` (OkHttp, `/v2/smoke/{ip}`), `CtiCacheRepository` (Room cache, smoke TTL 48h / fire TTL 6h), quota guardrails
- `gemini/` — `GeminiThreatAnalyzer` (firebase-ai via Firebase BOM, consent-gated, 5-min cooldown)
- `kismet/` — GPS delivery to Kismet over HTTP
- `sensor/` — `BarometerSampler`, `FloorEstimate`
- Activities: `MainActivity`, `ScanHistoryActivity`, `ScanMapActivity`, `ThreatResultsActivity`, `DiagnosticActivity`, `KismetSettingsActivity`

### Threat pipeline (3 layers)

```
Layer 1 — Local heuristics (HeuristicEngine)   — every scan, free
Layer 2 — CrowdSec CTI (/v2/smoke)             — IPs above suspicion threshold, cached
Layer 3 — Gemini (firebase-ai)                 — on-demand, consent-gated, quota-limited
```

CTI and Gemini are currently informational tracks; they do not yet feed back into confidence scoring (known gap — tracked in Known Runtime Gaps below).

### Desktop — flat ESM layout

All files are in `desktop/`. Module system is **ESM only** (`"type": "module"` in `package.json`).

| File | Role |
|------|------|
| `main.js` | Electron main process — IPC handlers, lifecycle |
| `preload.js` | Context bridge: `window.wscan` + `window.wscanDesktop` |
| `renderer.mjs` | Renderer UI |
| `scanner.mjs` | `iw dev <iface> scan` loop, parses `iw` output into AP objects |
| `detector.mjs` | `scoreAP()` / `scoreAPs()` — desktop-side threat scoring |
| `store.mjs` | Singleton `EventEmitter` state store (AP map, risk log, errors) |
| `companionServer.mjs` | `CompanionServer` — WebSocket server, token auth, rate limiting, monotonic-sequence validation |
| `adb/AdbTransport.js` | `AdbTransport` — connects to `WatchdogService` on `tcp:9000` via `@yume-chan/adb` |
| `adbPreflight.mjs` | ADB device readiness checks for UI |
| `sessionWorkbench.js` | JSON session artifact import |

### IPC flow (Electron)

`renderer` → `preload` (contextBridge) → `ipcMain.handle(...)` in `main.js` → `scanner.mjs` / `store.mjs` / `companionServer.mjs` / `AdbTransport`

Store emits events (`aps`, `riskLog`, `change`, `appError`) which `main.js` forwards to the renderer via `webContents.send`.

### Android ↔ Desktop communication

- **ADB transport**: `WatchdogService` binds `ServerSocket` on `localhost:9000`; desktop `AdbTransport` opens a forwarded TCP socket via `@yume-chan/adb`. First message is a JSON `hello` with `deviceId`, `capabilities`, `seq`, `sentAt`; desktop replies with `ack`.
- **WebSocket companion**: `CompanionServer` on port 47392 (configurable via `COMPANION_PORT` env). Android sends `scan` payloads with token auth after an `auth` handshake.

## Key Constraints

**AI split — do not violate:**
- Claude / Copilot: coding only
- Gemini / Vertex AI: in-app threat analysis (firebase-ai) — **do not remove, replace, or modify**

**Maps:** Current Android map is `LocalHeatmapView` — local/offline, no Google Maps SDK. MapLibre (free-only, OSM tiles) is the planned migration path and must be its own dedicated PR. Do not introduce Google Maps or any paid map provider.

**Security invariants:**
- Never use `exec()` with interpolated strings — always `spawn(cmd, [args])`
- Validate all arguments passed to child processes
- Never run Electron app as root
- Secrets only via `local.properties` (Android) or `.env` (desktop) — both are gitignored

**Node.js module system:** ESM only. All `package.json` files must have `"type": "module"`. No CommonJS.

**Kotlin toolchain:** AGP 9.x ships with built-in Kotlin — do not add `org.jetbrains.kotlin.android`. `jvmTarget` defaults from `compileOptions.targetCompatibility`.

| Component | Version |
|-----------|---------|
| AGP | 9.1.0 |
| Gradle | 9.4.0 |
| compileSdk / targetSdk | 36 |
| minSdk | 24 |
| JDK | 17 |
| ktlint | 14.2.0 |

## PR and Branch Workflow

- Branch names must start with an agent identifier prefix: `claude/`, `copilot/`, `gpt/`, `codex/`, or `dvntone/`
- One open PR at a time across all agents; every PR references exactly one GitHub Issue
- Open all PRs as **DRAFT** (`gh pr create --draft`); mark Ready for Review only when CI is green, all checklist items are complete, and no `blocker` or `security` issues are open
- Merge strategy: **squash only** (merge commits and rebase are disabled on this repo)
- All merges are performed manually by `@dvntone` — agents cannot merge PRs
- Max ~200 LOC changed per PR (tests excluded); no opportunistic refactors alongside feature/bug work
- Dependency additions or version changes must be their own dedicated PR (include license and any known CVEs in the description)
- If CI is red, fix it before any new feature work
- Never include device serials or persistent device identifiers in docs, issues, PR text, or commit messages

Every PR description must include: which agent made it, what changed and why, which rules guided decisions, and the quick-reference checklist from `docs/AGENTS.md`.

## Known Runtime Gaps (carry forward, not blockers)

- `knownProfiles` not yet populated from Room DB — three heuristics receive no historical data at runtime
- `environmentType` hardcoded to `RESIDENTIAL` — not inferred from context
- CTI and Gemini run as informational parallel tracks; they do not yet feed back into confidence scoring
- `CapabilityManifest` is probed and transported but not yet used in scoring weights

## PR Checklist (copy into every PR description)

- [ ] Made by: Claude
- [ ] References exactly one GitHub Issue
- [ ] CI is green before marking Ready for Review
- [ ] One feature OR one bugfix (not both)
- [ ] < 200 LOC changed (tests excluded)
- [ ] Meaningful tests included/updated
- [ ] Errors logged, not silently swallowed
- [ ] `.env` / `local.properties` NOT committed
- [ ] `git ls-files .env android/local.properties` returns empty
- [ ] Gemini/Vertex integration untouched (or issue filed)
- [ ] Merged via squash only
