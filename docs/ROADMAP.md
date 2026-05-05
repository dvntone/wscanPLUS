# wscan+ Roadmap

> Reassessed 2026-03-17. Android = companion scanner app feeding data to desktop hub. Not a standalone network monitor.

## Phase 0 — Foundation ✅ Complete

- [x] AI guardrails (AGENTS.md + copilot instructions)
- [x] Docs (INDEX, ROADMAP, DEPENDENCIES, SESSION_STATE)
- [x] Secrets scaffolding (.env.example, secrets.defaults.properties, .gitignore)
- [x] CI: Node lint/test + Android unit test + assembleDebug + secret scan
- [x] GitHub hygiene: branch protection, 1-PR-at-a-time, squash-only

PRs: #40, #43, #45, #47, #49, #51, #53, #55, #57

## Phase 1 — Dev Tooling + Polish ✅ Complete

Scanner chain + Android scaffold delivered, along with quality tooling and final polish.

- [x] WatchdogService + scanner chain (USB > Standard; Root dev-opt-in stub)
- [x] AndroidManifest permissions (full set for API 24-36)
- [x] Firebase AI Logic scaffold (firebase-bom:34.10.0 + firebase-ai)
- [x] Local offline scan heatmap scaffold
- [x] StandardScanner full implementation (API 24-29 + API 30+)
- [x] Wire scanner results into WatchdogService
- [x] ktlint 14.2.0 CI (`:core` + `:app`)
- [x] App icon + `android:dataExtractionRules`
- [x] WatchdogService 6-hour restart handling (Android 15+ dataSync limit)
- [x] Settings deep link on permission denial
- [x] `:core` unit test baseline

PRs: #62-#77 (scanner chain), #84 (ktlint), #135, #137

> **Scanner chain:** USB > Standard. Root = dev opt-in stub only. Nexmon removed (Broadcom-only). Shizuku removed (zero scanning capability).

## Phase 2 — Local Threat Intelligence (Android) ✅ Complete baseline

On-device heuristics engine is now live on Android and runs without third-party API calls.

- [x] ThreatSignal + ThreatSource-oriented heuristic pipeline shape locked
- [x] Room database baseline added for scan history accumulation
- [x] OUI asset support scaffolded for vendor identification
- [x] Heuristic engine:
  1. WEP/Open network detection
  2. Evil twin detection
  3. Encryption downgrade detection
  4. Karma-style multi-SSID / single-BSSID detection
  5. SSID flooding detection
  6. RSSI anomaly detection
  7. BSSID fingerprinting
- [x] Policy gate wired into the watchdog path
- [x] App-side logging added for adb/runtime verification
- [x] Device validation completed across Revvl Tab 2, moto g play - 2024, and Pixel 10 Pro XL for the pre-fix and mixed-fix permission/service work to date; Revvl still needs a post-fix re-test on current main

Delivered through PRs `#96-#117`, `#127`, and follow-on device-validation/docs work on 2026-03-20.

### Runtime hardening (in-progress, tracked separately from phase completion)

Phase 2 is complete. The following items are known gaps between the architecture and the current runtime behavior. They are tracked as drift corrections, not new features:

- [ ] Wire Room DB into `ScanContext` at decision time — `knownProfiles`, `baselineNetworkCount`, `baselineStdDev` are always empty/null at runtime; three heuristics (EncryptionDowngrade, BssidFingerprint, SsidFlooding) receive no historical data
- [ ] Remove hardcoded `EnvironmentType.RESIDENTIAL` in `WatchdogService` — infer from context or default to `UNKNOWN`
- [ ] Heuristic-aware gate hardening in `PolicyGate` — recurrence/corroboration weighting for behavioral detections; structural detections (WEP/Open) always pass
- [ ] False-positive brakes backed by real CTI/context instead of stub logic
- [ ] Capability-aware scoring weight once context wiring is stable

### Locked permission stance from device validation

- `ACCESS_FINE_LOCATION` is the required scan-capable path on current Android targets.
- `ACCESS_COARSE_LOCATION` remains supported only as degraded onboarding / limited mode, not normal scan-capable operation.
- `ACCESS_BACKGROUND_LOCATION` is required for the intended field / long-running detection mode because foreground-only access is not sufficient for reliable background and keyguard continuity.

## Phase 3 — Privacy + CTI Integration (Android) ✅ Complete

- [x] Consent framework (opt-in, GDPR/CCPA compliant) (PR #173)
- [x] CrowdSec CTI client (OkHttp, `/v2/smoke/{ip}`) (PR #174)
- [x] Firebase setup (BOM 34.11.0) (PR #175)
- [x] CTI cache + quota guardrails + degraded-mode handler (PRs #177, #180)
- [x] Local offline threat heatmap + GPS-tagged scan history
- [x] SQLCipher AES-256 DB encryption + 30-day retention purge (PR #181)

PRs: #173–#182

## Phase 4 — AI Layer + Reporting (Android) ✅ Complete

- [x] GeminiThreatAnalyzer (firebase-ai, consent-gated, 5-min cooldown) (PR #183)
- [x] GeminiNarrativeEntity + DAO + DB v4 (PR #184)
- [x] ThreatResultsActivity + bug fixes (PR #186)
- [x] Scan history export (JSON) (PR #187)
- [x] Scan history timeline activity (PR #190)
- [x] ADB transport — device list + WatchdogService port forward tcp:9000 (PRs #192–#193)
- [x] Companion shell + Android session artifact import (PRs #194–#195)

PRs: #183–#195

## Phase 5 — Desktop Hub + Companion Sync

Desktop receives and aggregates data from Android companion(s).

### Complete
- [x] Desktop flat layout + IPC bridge (PR #200)
- [x] ADB transport — `@yume-chan/adb`, WatchdogService tcp:9000 (PRs #192–#195)
- [x] Android ServerSocket(9000) implementation (PR #193)
- [x] CompanionServer (WebSocket, token auth, rate limiting) (PR #200)
- [x] Scanner + detector + store wired into Electron main (PR #200)
- [x] CapabilityProbe layer — DeviceCapabilityManifest + DetectorGate + self-tests (PR #198)
- [x] Transport hello — DeviceCapabilityManifest serialized in hello (PR #209 Android, PR #211 desktop)
- [x] Spatial WiFi floor tracking — BarometerSampler + FloorEstimate (PR #217, #219, #221)

### Remaining
- [ ] darklotusLABS web UI — Sentinel Prism theme, NYX assistant, Vite build (app.darklotuslabs.com)
- [ ] Desktop CTI client (fetch-based)
- [ ] Unified timeline + map overlay

> **Authoritative state:** See SESSION_STATE.md. This checklist may lag.

## Phase 6 — Packaging + Release

- [ ] Android signed release + R8
- [ ] Linux AppImage + .deb
- [ ] Setup wizards
- [ ] API key hardening + rotation schedule
- [ ] Biometric auth (now there's data to protect)
- [ ] Play Store submission

~8-10 PRs

---

## Future Features — Post-Release

### Android Bubble (Persistent Threat Indicator)

Floating threat-level bubble overlay while using other apps, modeled on Google Messages chat heads.

- `BubbleMetadata` + `NotificationChannel.setAllowBubbles(true)` (API 30+)
- Bubble icon reflects current threat level (color-coded: green/yellow/red)
- Tap expands to condensed scan summary (top threat, network count, floor)
- `BubbleActivity` declared with `android:allowEmbedded="true"`
- Requires user to grant bubble permission per-app
- **Device compatibility note:** confirmed working on Pixel; not supported on Moto G Play 2024 (XT2613-1)
- Phase 1: indicator only (tap → MainActivity). Phase 2: embedded mini scan view.

---

## Deferred — Not in Scope Near-Term

### VpnService / Network Traffic Pipeline

Companion app, not a network monitor. Revisit only if product direction changes.

- Would require: VpnService, local packet parsing (DNS/IP/SNI), Play Store VPN review
- Reference: PCAPdroid (FOSS VpnService NIDS, F-Droid)

### ADB-Elevated Detection (post-beta, F-Droid/sideload only)

- `READ_LOGS` — deauth detection via `wpa_supplicant` reason codes
- `WRITE_SECURE_SETTINGS` — disable scan throttling, force Private DNS
- `DUMP` — `dumpsys wifi` for roaming history, RSSI polling
- Not Play Store compatible — F-Droid/sideload distribution only

### Pixel / Advanced Protection First-Trust Onboarding

- Current evidence covers an already-trusted host only
- Still needs a dedicated validation pass for:
  - first host authorization
  - install/update behavior before trust is established
  - operator guidance around trusted hosts and USB debugging prompts
- Important for the later desktop companion onboarding flow, but not a blocker for the current Android scanner architecture

### On-Device ML (LiteRT / Offline LLM)

- LiteRT for anomaly detection on time-series disconnect data
- Gemma 2B via MediaPipe for offline contextual analysis
- Current architecture: local heuristics + cloud Gemini. On-device ML is a future tier.

---

## Removed from Planning

- **Detekt** — removed 2026-03-17, replaced by ktlint
- **NetKit / lamco.ai** — AI-generated repo, not viable
- **VpnService** — deferred (companion app, not network monitor)
- **Biometric auth** — deferred to Phase 6 (nothing to protect yet)
- **TrustKit** — SSL pinning, not WiFi threat analysis
- **NetCipher** — Tor routing, not threat detection
- **Shizuku** — zero scanning capability, fragile on Android 16+
- **Nexmon** — Broadcom-only, incompatible with test devices
