# wscan+ — What This Project Is and Where It Stands

*Written 2026-03-23. Plain language. No jargon.*

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

## What's actually built and working

**Phase 1 — Scanner foundation (complete)**
- WatchdogService: foreground service that keeps scanning alive across background/keyguard/Android 15 restrictions
- Scanner chain: USB adapter (priority) → Standard WiFi scan. Root is dev-only opt-in, never a silent fallback.
- Passive scanning works even with WiFi radio OFF (wifi_scan_always_enabled)
- Permission handling for Android 7–15 across fine/coarse/background location
- ADB transport: Android listens on localhost:9000, desktop connects via ADB (Phase 3 implementation)
- App icon, versioning (v0.1.0), data extraction rules, settings deep links

**Phase 2 — Local threat detection (complete)**
- 7 WiFi threat heuristics running on every scan:
  1. WEP/Open network detection
  2. Evil twin detection (5 composite signals: OUI mismatch, security mismatch, channel, new BSSID, RSSI)
  3. Encryption downgrade (WPA2/3 → Open/WEP)
  4. Karma attack (WiFi Pineapple — multiple SSIDs on one BSSID)
  5. SSID flooding (beacon spam — z-score vs rolling baseline)
  6. RSSI proximity anomaly (transmitter in same room)
  7. BSSID fingerprint rotation (attacker hardware swap)
- HeuristicEngine coordinator + PolicyGate (confidence threshold 0.3)
- Room database: scan sessions, scan results, BSSID fingerprints, threat signals, CTI cache
- OUI vendor lookup (IEEE database bundled as asset)
- ~100 unit tests passing

**What's NOT built yet (Phase 3+)**
- CTI integration (CrowdSec API — IP reputation lookup)
- Gemini AI threat narrative generation (firebase-ai)
- Scan history accumulation and baseline population
- Desktop hub ADB library (deferred — ESM compatibility issue)
- Google Maps threat heatmap (issue #9)
- VpnService network traffic pipeline (deferred — out of scope for companion app)

---

## Current blockers

Three P1 Android 15 bugs on Revvl Tab 2 that need resolution before new features:
- `#122` — missing app-side ADB logs on Android 15
- `#124` — coarse-only location fails scan retrieval
- `#125` — backgrounded scan loses location access

These do not affect the Moto G Play (API 34) or Pixel 10 Pro XL.

---

## The agents

- **Claude** — primary coding agent. Writes everything, opens PRs, merges. Runs with your GitHub token.
- **Codex** — secondary. Activated only when Claude gives you a prompt to trigger it.
- **Copilot** — reviews PRs automatically. Must be checked before merging.
- **Gemini** — in-app threat analysis only (firebase-ai). Not a coding agent. Cannot see this repo.
- **@dvntone** — direction, approval, final call. Does not write code.

---

## What comes next (Phase 3)

1. Fix the 3 P1 Android 15 bugs
2. Scan history accumulation — populate knownProfiles and baseline network counts
3. CTI client — CrowdSec API with Room cache (smoke TTL 48h, fire TTL 6h)
4. Gemini AI analysis layer — plain-language threat narrative from combined heuristic + CTI signal
5. ADB desktop library decision (Tango ADB ESM evaluation)

---

*This document reflects the actual state of the repo as of 2026-03-23. It is not aspirational — everything in "what's built" is merged to main and verified.*
