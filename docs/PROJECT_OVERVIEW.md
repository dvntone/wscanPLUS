# wscan+ — What This Project Is and Where It Stands

> **State as of 2026-04-22.**
> For implementation decisions, always use [SESSION_STATE.md](SESSION_STATE.md) as the authoritative source.
> This document gives a plain-language overview only.

---

## Why this exists

You live in veterans housing where an attacker — real, local, persistent — has been running rogue WiFi hotspots, deauth floods, and credential-capture portals against residents who are older, disabled, and have no way to know what's happening to them. The problem isn't just detection. It's being able to explain it to people who can't read a packet capture, and being able to document it in a way that can be shown to building management or support services.

wscan+ is built to close that gap.

---

## What it is

An Android app that runs on your phone and detects WiFi-layer attacks in real time — silently, in the background, without requiring the WiFi radio to be on. It speaks plain English to the user. It stores evidence. It connects to a desktop hub for deeper analysis.

**Three components:**

1. **Android companion app** (Kotlin) — the field sensor. Runs on any Android 7+ device. Scans passively, detects threats, stores results locally, syncs to desktop.
2. **Desktop hub** (Electron, Linux-first) — aggregates data from one or more Android devices, runs deeper analysis, integrates with Kismet/BetterCap for professional use.
3. **Web UI** (PWA) — dashboard served locally by the desktop. Not a standalone product.

---

## What's built (current state — Phases 0–4 complete)

**Phase 0–1 — Foundation + scanner (complete)**
- WatchdogService: foreground service, survives background/keyguard/Android 15 restrictions
- Scanner chain: USB adapter (priority) → Standard WiFi scan. Root is dev-only opt-in.
- Permission handling for Android 7–15 (fine/coarse/background location)
- App icon, versioning (v0.1.0), data extraction rules, settings deep links

**Phase 2 — Local threat detection (complete)**
- 7 WiFi threat heuristics running on every scan:
  1. WEP/Open network detection
  2. Evil twin detection (5 composite signals)
  3. Encryption downgrade
  4. Karma attack (WiFi Pineapple)
  5. SSID flooding (beacon spam, z-score vs rolling baseline)
  6. RSSI proximity anomaly
  7. BSSID fingerprint rotation
- HeuristicEngine + PolicyGate (confidence threshold 0.3)
- Room database: scan sessions, results, BSSID fingerprints, threat signals, CTI cache
- OUI vendor lookup (IEEE database bundled as asset)
- ~100 unit tests passing

**Phase 3 — Privacy + CTI integration (complete)**
- Consent framework (opt-in, GDPR/CCPA compliant)
- CrowdSec CTI client (OkHttp, `/v2/smoke/{ip}`, quota guardrails, degraded-mode handler)
- CTI Room cache (smoke TTL 48h, fire TTL 6h)
- Google Maps threat heatmap + GPS-tagged scan history
- SQLCipher AES-256 database encryption + 30-day retention purge

**Phase 4 — AI layer + reporting (complete)**
- GeminiThreatAnalyzer (firebase-ai, consent-gated, 5-min cooldown)
- Scan history timeline activity
- Scan history export (JSON)
- ADB transport (WatchdogService tcp:9000, `@yume-chan/adb` on desktop)
- Companion shell + Android session artifact import

**Phase 5 — Desktop hub + companion sync (partial)**
- Desktop flat layout + IPC bridge ✅
- CompanionServer (WebSocket, token auth, rate limiting) ✅
- Scanner + detector + store wired into Electron main ✅
- CapabilityProbe layer (DeviceCapabilityManifest + DetectorGate) ✅
- Transport hello with capability manifest ✅
- Barometer-based floor tracking ✅
- **Remaining:** darklotusLABS web UI (Sentinel Prism theme, NYX assistant, Vite build)
- **Remaining:** Desktop CTI client (fetch-based)
- **Remaining:** Unified timeline + map overlay

---

## Known hardening items (in-progress, not blockers)

These are real gaps between what the architecture describes and what the runtime currently does:

- `knownProfiles` is not yet populated from Room DB at decision time — three heuristics (EncryptionDowngrade, BssidFingerprint, SsidFlooding) are structurally present but receive no historical data
- `environmentType` defaults to `RESIDENTIAL` rather than being inferred from context
- CTI and Gemini run as parallel informational tracks; they do not yet feed back into confidence scoring
- `CapabilityManifest` is probed and transported but not yet used in scoring weights

These are tracked as runtime drift corrections and will be addressed before further feature expansion.

---

## The agents

- **Claude** — primary coding agent. Opens PRs, writes code. Uses `claude/` branch prefix.
- **Copilot/Codex** — secondary coding agent. Uses `copilot/` branch prefix.
- **Gemini** — in-app threat analysis only (firebase-ai). Not a coding agent.
- **@dvntone** — direction, approval, final call. Only person who merges.

---

## What comes next

Per [SESSION_STATE.md](SESSION_STATE.md) (authoritative):

1. Documentation alignment (underway)
2. Runtime drift correction — wire Room/history into ScanContext, remove hardcoded environment defaults, harden PolicyGate
3. Phase 5 remaining work — darklotusLABS web UI, desktop CTI client, unified timeline
4. Device validation matrix across test devices before broader UI expansion
