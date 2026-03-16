# wscan+ Roadmap

## Phase 0 — Foundation ✅ Complete
- [x] Add AI guardrails (AGENTS.md + copilot instructions)
- [x] Consolidate docs (INDEX + ROADMAP + DEPENDENCIES + SESSION_STATE)
- [x] Secrets scaffolding (.env.example, secrets.defaults.properties, .gitignore)
- [x] Minimal CI only:
  - Node: lint + tests
  - Android: unit tests + build
  - Secret scanning (TruffleHog or equivalent)
  - Job + step timeouts
- [x] GitHub hygiene:
  - Labels, milestones, project board
  - One-PR-at-a-time policy enforced socially + branch protection

## Phase 1 — Android (ship a solid standalone scanner) — In Progress
- [ ] WatchdogService stub + scanner chain skeleton (USB > Standard; Root dev-opt-in only)
- [ ] AndroidManifest permissions (USE_BIOMETRIC, ACCESS_FINE_LOCATION, FOREGROUND_SERVICE, FOREGROUND_SERVICE_DATA_SYNC, INTERNET, NEARBY_WIFI_DEVICES)
- [ ] Firebase AI Logic scaffold (firebase-bom + firebase-ai, google-services plugin)
- [ ] Google Maps-based scan history + threat heatmap
- [ ] Biometric lock (protect local history + keys)
- [ ] VPN-based “disconnect anomaly” detector (worded conservatively; not raw 802.11 deauth frames)
- [ ] Export: JSON + optional PCAP for captured flows (IP-layer only) / logs

> **Scanner chain (confirmed 2026-03-16):** USB > Standard. Root = dev opt-in stub only, never silent fallback. Nexmon removed (Broadcom-only). Shizuku removed (zero scanning capability).

## Phase 2 — Linux Desktop Hub (professional workflow)
- [ ] Passive scan + heuristic engine
- [ ] Connectivity detector (dual adapters / USB tether / ADB presence)
- [ ] Optional monitor-mode session manager (advanced; explicit user consent)
- [ ] PCAP analysis pipeline via tshark (post-capture)
- [ ] PWA dashboard served locally

## Phase 3 — Companion Sync
- [ ] Android -> Desktop streaming (ADB USB first, then LAN)
- [ ] Unified timeline + map overlay of mobile scans

## Phase 4 — AI Layer
- [ ] Standardized Gemini prompt templates + versioning
- [ ] Per-scan summary + recommendations
- [ ] Offline queue + later analysis

## Phase 5 — Packaging / Release
- [ ] Android signed release pipeline working
- [ ] Linux packaging (AppImage/.deb)
- [ ] Setup wizard for dependencies and permissions
