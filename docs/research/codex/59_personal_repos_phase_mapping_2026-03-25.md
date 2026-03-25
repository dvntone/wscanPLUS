# Personal Repos — Phase Mapping, Completion %, Next Critical Phase

**Date:** 2026-03-25
**Scope:** All 9 dvntone GitHub repos mapped against the wscanplus roadmap
**Ordered by:** Priority / importance to overall project

---

## Summary Table

| Priority | Repo | Phase | Completion | Status |
|----------|------|-------|------------|--------|
| 1 | wscanplus | Phase 2→3 | Phase 2: ~85% / Phase 3+: 0% | Active |
| 2 | wscanplus_desktop | Phase 5 | ~5% | Active, CI broken |
| 3 | wscanplus_webui | Phase 5 | ~2% | Active, blocked on desktop |
| 4 | wifisentry | Superseded | N/A | Archived — 12 open issues |
| 5 | wscanplus-deprecrated- | Deprecated | N/A | 3 open issues — triage needed |
| 6 | AndGemKal-alpha | Phase 4 prototype | Alpha stub | Stale since Oct 2025 |
| 7 | flipp3d | Out of scope | N/A | Private, unrelated project |
| 8 | studio | N/A | 0% | Empty scaffold |
| 9 | codeleap-app | N/A | 0% | Never pushed |

---

## Per-Repo Detail

### 1. dvntone/wscanplus — Priority 1

**Role:** Primary Android companion app — the field sensor. Core of the entire system.

**Phase breakdown:**

| Phase | Status | Completion | Key gaps |
|-------|--------|------------|----------|
| 0 — Foundation | ✅ Complete | 100% | — |
| 1 — Scanner + Tooling | ✅ Complete | 100% | — |
| 2 — Local Threat Intelligence | Baseline complete, follow-on pending | ~85% | `knownProfiles`, baseline stats, falsePositiveBrakes |
| 3 — CTI Integration | Not started | 0% | CrowdSec client, consent framework, CTI cache |
| 4 — AI + Reporting | Not started | 0% | firebase-ai scaffolded but not wired |
| 5 — Desktop Sync | Stub only | ~2% | ServerSocket(9000) is a TODO comment |
| 6 — Release | Not started | 0% | Signed APK, R8, Play Store |

**Phase 2 gaps (grounded in code):**
- `WatchdogService.kt:155` — `knownProfiles = emptyMap()` — BssidFingerprintHeuristic and RssiAnomalyHeuristic run with zero historical context
- `WatchdogService.kt:156–157` — `baselineNetworkCount = null`, `baselineStdDev = null` — SsidFloodingHeuristic returns null on every scan (no baseline = no signal)
- `WatchdogService.kt:158` — `environmentType = EnvironmentType.RESIDENTIAL` hardcoded — RSSI thresholds never adapt
- `PolicyGate.kt` — `falsePositiveBrakes` flag declared but logic not implemented

**Open issues:** 4 (includes P1 Android 15 bugs #122, #124, #125 on Revvl Tab 2)

**Next critical:** Scan history accumulation — query Room DB at session start, populate `knownProfiles`, `baselineNetworkCount`, `baselineStdDev`. Unblocks 3 heuristics simultaneously and is prerequisite for Phase 3.

---

### 2. dvntone/wscanplus_desktop — Priority 2

**Role:** Desktop hub. Receives scan data from Android via ADB port forwarding, aggregates across devices, serves web UI locally. Electron app, Linux-first.

**Phase:** Phase 5 in wscanplus roadmap.

**Planning status:** Architecture fully designed in wscanplus codex:
- `01_data_exchange_contract.md` — NDJSON message protocol over ADB localhost:9000
- `03_desktop_hub_requirements.md` — multi-device aggregation, timeline, CTI client, evidence export
- 7 message schema gaps documented (handshake, versioning, retry rules, backlog sync, correlation, evidence bundle format, Kali validation plan)

**Completion: ~5%**
- Electron scaffold exists ✅
- CI configured ✅
- CodeQL workflows broken ❌ (SESSION_STATE.md: requires `gh auth refresh -s workflow`)
- No ADB library integrated
- No socket listener (Android ServerSocket not open either)
- No scan aggregation, timeline, map overlay, or CTI client

**Next critical:**
1. Fix CodeQL CI (`gh auth refresh -s workflow`) — immediate, unblocks PRs
2. ADB library evaluation — Tango ADB ESM compatibility (deferred pending Phase 3 complete)
3. No implementation work until Android Phase 3 is done

---

### 3. dvntone/wscanplus_webui — Priority 3

**Role:** PWA dashboard served locally by the desktop hub. Not a standalone product — it has no meaning without wscanplus_desktop running.

**Phase:** Phase 5 (dependent on wscanplus_desktop).

**Planning status:** Referenced in PROJECT_OVERVIEW.md as "Web UI (PWA) — dashboard served locally by the desktop." No design doc beyond that sentence.

**Completion: ~2%**
- Initial scaffold committed ✅
- CodeQL CI broken ❌ (same issue as desktop)
- No UI components, no data binding, no API contract with desktop defined

**Next critical:** Entirely blocked on wscanplus_desktop being built. Fix CodeQL CI. No independent implementation work possible yet.

---

### 4. dvntone/wifisentry — Priority 4

**Role:** Archived predecessor to wscanplus. Kotlin Android app, the original iteration of the WiFi threat detection concept.

**Phase:** Superseded. Archived 2026-03-09, replaced by wscanplus (created 2026-03-10 — one day later).

**Completion:** Unknown at archive time.

**Notable:** 12 open issues were never resolved before archiving. Some may contain threat detection ideas, bug patterns, or design decisions that are directly relevant to current wscanplus development. Has GitHub Pages enabled (documentation site may still be live).

**Next critical:** Triage the 12 open issues. Migrate any that are still valid to dvntone/wscanplus before the context is lost. Archive cleanup is low priority but the issue triage is worth doing once.

---

### 5. dvntone/wscanplus-deprecrated- — Priority 5

**Role:** Early test scaffold, description "Test". Kotlin. Deprecated predecessor.

**Phase:** Deprecated.

**Completion:** N/A. 3 open issues unresolved.

**Next critical:** Review 3 open issues — close as won't-fix or migrate to dvntone/wscanplus. Archive the repo.

---

### 6. dvntone/AndGemKal-alpha — Priority 6

**Role:** Private alpha experiment described as "gemKAL Test." Name suggests Gemini + Kali Linux combination. Very small (8 bytes), stale since October 2025.

**Phase:** Prototype for wscanplus Phase 4 (Gemini AI threat narrative integration via firebase-ai).

**Completion:** Alpha stub only.

**Next critical:** Review before Phase 4 Gemini work begins — any prompt structures, API call patterns, or Kali integration notes may be useful. Archive after review.

---

### 7. dvntone/flipp3d — Priority 7

**Role:** Private TypeScript project. Confirmed unrelated to wscanplus.

**Phase:** Out of scope.

**Next critical:** None from wscanplus perspective.

---

### 8. dvntone/studio — Priority 8

**Role:** Empty public scaffold. 0 bytes. Created October 2025, no pushes since.

**Phase:** N/A.

**Next critical:** Archive or delete.

---

### 9. dvntone/codeleap-app — Priority 9

**Role:** Private Android coding tutorial. Never had any code pushed.

**Phase:** N/A.

**Next critical:** Archive or delete.

---

## Action Items by Urgency

### Now (before next wscanplus feature work)
- **wscanplus**: Resolve P1 bugs #122, #124, #125 (Android 15 Revvl Tab 2)
- **wscanplus_desktop + wscanplus_webui**: Fix CodeQL CI (`gh auth refresh -s workflow`)

### Soon (Phase 2 follow-on)
- **wscanplus**: Scan history accumulation — populate `knownProfiles`, `baselineNetworkCount`, `baselineStdDev` from Room DB

### Before Phase 4
- **AndGemKal-alpha**: Review for Gemini prompt patterns, then archive

### Housekeeping (low urgency)
- **wifisentry**: Triage 12 open issues, migrate valid ones to wscanplus
- **wscanplus-deprecrated-**: Triage 3 open issues, close or migrate, then archive
- **studio**: Archive or delete
- **codeleap-app**: Archive or delete

---

## The Three Active Repos as a System

```
wscanplus (Android)          wscanplus_desktop (Electron)     wscanplus_webui (PWA)
Phase 2→3, ~85% Ph2          Phase 5, ~5%                     Phase 5, ~2%
Field sensor                 Hub + aggregation                 Dashboard UI
     |                              |                                |
     | NDJSON/ADB :9000             | serves locally                 |
     └──────────────────────────────┘────────────────────────────────┘

Android must complete Phase 3 before desktop/webui development makes sense.
```
