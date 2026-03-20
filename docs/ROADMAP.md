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

Scanner chain + Android scaffold complete. Now: quality tooling + remaining polish.

- [x] WatchdogService + scanner chain (USB > Standard; Root dev-opt-in stub)
- [x] AndroidManifest permissions (full set for API 24-36)
- [x] Firebase AI Logic scaffold (firebase-bom:34.10.0 + firebase-ai)
- [x] Google Maps scaffold (play-services-maps:20.0.0)
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
- [x] Device validation completed across Revvl Tab 2, moto g play - 2024, and Pixel 10 Pro XL for the current permission/service model

Delivered through PRs `#96-#117`, `#127`, and follow-on device-validation/docs work on 2026-03-20.

### Phase 2 follow-on items deferred beyond the baseline

These are real remaining tasks, but they are no longer reasons to treat Phase 2 as mostly incomplete:

- [ ] Scan history accumulation to populate `knownProfiles` and network baselines
- [ ] OuiAssetLoader integration in `WatchdogService` so BSSID vendor lookup is no longer wired as `null`
- [ ] False-positive brakes backed by real CTI/context instead of stub logic
- [ ] DAO instrumentation tests using an Android emulator path
- [ ] Additional pure-logic unit tests around heuristic edge cases

### Locked permission stance from device validation

- `ACCESS_FINE_LOCATION` is the required scan-capable path on current Android targets.
- `ACCESS_COARSE_LOCATION` remains supported only as degraded onboarding / limited mode, not normal scan-capable operation.
- `ACCESS_BACKGROUND_LOCATION` is required for the intended field / long-running detection mode because foreground-only access is not sufficient for reliable background and keyguard continuity.

## Phase 3 — Privacy + CTI Integration (Android)

External API calls require consent framework first. GDPR/CCPA compliance before any third-party data sharing.

- [ ] Consent framework (opt-in, GDPR/CCPA compliant) — BEFORE any API calls
- [ ] CrowdSec CTI client (OkHttp, `/v2/smoke/{ip}`)
- [ ] CTI cache (Room, smoke 48h / fire 6h TTL)
- [ ] Quota guardrails (30 req/week free tier)
- [ ] Degraded-mode handler (cache-only when CTI unavailable)

~8-10 PRs

## Phase 4 — AI Layer + Reporting (Android)

Gemini integration for natural language threat assessment + user-facing results.

- [ ] Gemini/firebase-ai runtime integration (prompt builder consuming ThreatSignal list)
- [ ] Incident Narrative generator (Gemini converts events to plain English)
- [ ] Baseline vs. Now visual (normal network count vs. incident density)
- [ ] Threat results UI (list, detail, color-coded badges)
- [ ] Scan history timeline (Room-backed)
- [ ] Google Maps heatmap (GPS-tagged scans, cluster markers)
- [ ] JSON export (scan session + ThreatSignals + audit log)
- [ ] Plain-language incident summary export (shareable)
- [ ] Thermal/power correlation logging (battery drain alongside scan events)

~10-14 PRs

## Phase 5 — Desktop Hub + Companion Sync

Desktop receives and aggregates data from Android companion(s).

- [ ] Desktop `src/` structure + IPC bridge
- [ ] ADB library evaluation (Tango ADB for ESM compatibility)
- [ ] Android ServerSocket(9000) implementation
- [ ] Desktop scan aggregation + sqlite cache
- [ ] Desktop CTI client (fetch-based)
- [ ] Unified timeline + map overlay

~12-16 PRs

## Phase 6 — Packaging + Release

- [ ] Android signed release + R8
- [ ] Linux AppImage + .deb
- [ ] Setup wizards
- [ ] API key hardening + rotation schedule
- [ ] Biometric auth (now there's data to protect)
- [ ] Play Store submission

~8-10 PRs

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
