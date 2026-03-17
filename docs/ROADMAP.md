# wscan+ Roadmap

> Reassessed 2026-03-17. Android = companion scanner app feeding data to desktop hub. Not a standalone network monitor.

## Phase 0 — Foundation ✅ Complete

- [x] AI guardrails (AGENTS.md + copilot instructions)
- [x] Docs (INDEX, ROADMAP, DEPENDENCIES, SESSION_STATE)
- [x] Secrets scaffolding (.env.example, secrets.defaults.properties, .gitignore)
- [x] CI: Node lint/test + Android unit test + assembleDebug + secret scan
- [x] GitHub hygiene: branch protection, 1-PR-at-a-time, squash-only

PRs: #40, #43, #45, #47, #49, #51, #53, #55, #57

## Phase 1 — Dev Tooling + Polish — In Progress

Scanner chain + Android scaffold complete. Now: quality tooling + remaining polish.

- [x] WatchdogService + scanner chain (USB > Standard; Root dev-opt-in stub)
- [x] AndroidManifest permissions (full set for API 24-36)
- [x] Firebase AI Logic scaffold (firebase-bom:34.10.0 + firebase-ai)
- [x] Google Maps scaffold (play-services-maps:20.0.0)
- [x] StandardScanner full implementation (API 24-29 + API 30+)
- [x] Wire scanner results into WatchdogService
- [x] ktlint 14.2.0 CI (`:core` + `:app`)
- [ ] App icon + `android:dataExtractionRules`
- [ ] WatchdogService 6-hour restart (Android 15+ dataSync limit)
- [ ] Settings deep link on permission denial
- [ ] First unit tests for `:core` module

PRs: #62-#77 (scanner chain), #84 (ktlint)

> **Scanner chain:** USB > Standard. Root = dev opt-in stub only. Nexmon removed (Broadcom-only). Shizuku removed (zero scanning capability).

## Phase 2 — Local Threat Intelligence (Android)

On-device heuristics engine — no API calls, no privacy concerns, runs every scan.

- [ ] ThreatSignal + ThreatSource data model (shape locked in SESSION_STATE)
- [ ] Room database for scan history + BSSID fingerprinting (first-seen/last-seen)
- [ ] OUI database — bundle IEEE `oui.csv` for vendor identification
- [ ] Heuristic engine:
  1. WEP/Open network detection (parse `capabilities` string)
  2. Evil twin (duplicate SSID, different BSSID + OUI vendor mismatch)
  3. Encryption downgrade (known WPA3/WPA2 now broadcasting Open/WEP)
  4. Karma attack (multiple SSIDs sharing single BSSID or same channel+RSSI)
  5. SSID flooding (abnormally high network count vs. stored baseline)
  6. RSSI delta anomaly (signal > -30dBm in residential = proximity alert)
  7. BSSID fingerprinting (track attacker hardware across SSID rotations)
- [ ] Policy gate — score-based escalation thresholds
- [ ] False-positive brakes — known corp ASN + clean signals = suppress
- [ ] Unit tests for each heuristic (pure logic, no Android deps)
- [ ] Wire into WatchdogService

~8-10 PRs

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
