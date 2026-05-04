# Handoff: wscan+ Code Review Fixes

**Repo:** `dvntone/wscanplus`
**Date:** 2026-05-03
**Prepared by:** Claude (Anthropic) — prior review session

---

## Overview

A prior review session audited desktop (**Node/Electron**) and Android (**Kotlin**) code and produced follow-up candidates. This handoff is a triage queue, not a mandate to batch everything in one change.

Process these as **separate** tracked follow-ups:
- 1 PR at a time; use the author-appropriate branch prefix: `claude/`, `copilot/`, or `dvntone/`
- one bugfix/work unit per PR
- keep PRs small and policy-compliant

**Policy reminders (from `AGENTS.md` / `docs/SESSION_STATE.md`):**
- 1 PR at a time; branch prefix must be `claude/`, `copilot/`, or `dvntone/` (by author)
- Tests-first; CI must be green before marking PR ready
- No `exec()` with interpolated strings — use `spawn(cmd, [args])`
- Squash-merge only; PRs are draft until CI is green
- No secrets committed; no new CI scanners
- Read `docs/SESSION_STATE.md`, `AGENTS.md`, and `KNOWN_ISSUES.md` before starting

---

## Issues to Fix

Issues are ordered from simplest/safest to most nuanced.

---

### Desktop — `desktop/`

#### D-1 · Typo in exported constant name

**File:** `desktop/adbPreflight.mjs` line 135; `desktop/main.js` line 8 and line 319

`PRELIGHT_CLASSIFICATIONS` is missing the letter `F` — should be `PREFLIGHT_CLASSIFICATIONS`. Rename consistently in both files and in any tests (`desktop/adbPreflight.test.js`).

---

#### D-2 · In-flight scan results may arrive after `stopScanning()`

**File:** `desktop/scanner.mjs` lines 239–243

`stopScanning()` clears the timer and sets `scanning: false` but does not abort `activeScanPromise`. A scan that was mid-flight when the user pressed stop still completes and calls `store.addAps()` / `store.addRiskEntries()`, pushing phantom updates to the renderer.

**Fix:** Gate discarded results **before** `store.addAps()` / `store.addRiskEntries()` (or before equivalent post-processing after `executeScanCycle()` returns). A `.finally()`-only check is insufficient because store writes happen inside `runScanAndProcess()`.

---

#### D-3 · Pairing address contract: verify object-vs-string expectations before changing IPC shape

**File:** `desktop/companionServer.mjs` line 51; `desktop/main.js` lines 394, 407, 411

`CompanionServer.address` getter returns `this.#httpServer?.address() ?? null`. For a bound TCP server, `http.Server.address()` returns `{ address, family, port }`, and current pairing UI code consumes object fields (`result.address.address` + `result.address.port`).

- `companion:pair` → `{ ok, token, address }` (line 394)
- `companion:generateToken` → `{ token, address }` (line 407)
- `companion:status` → `{ running, address, token }` (line 411)

**Fix:** Do **not** blindly convert this to a single string. If you change payload shape, update all consumers together (notably `desktop/pairing.js`) or preserve backward compatibility by including both object fields and a formatted endpoint/url string.

---

#### D-4 · N full renders triggered for a single risk-log payload

**File:** `desktop/renderer.mjs` lines 422–424; `addRisk` at line 109

The `onRiskLog` subscriber calls `addRisk(risk)` for every entry in the snapshot. `addRisk` always calls `render()`. For a snapshot with N entries this causes N sequential full DOM re-renders.

**Fix:** Refactor `addRisk` to accept an optional `rerender = true` parameter (mirroring how `addEvent` already works at line 112), then call `addRisk(risk, false)` inside the loop and call `render()` once after the loop completes.

---

#### D-5 · Non-constant-time token comparison

**File:** `desktop/companionServer.mjs` line 98

`msg.token !== this.#token` is a plain JS string equality check used for WebSocket authentication.

**Fix:** Replace with `crypto.timingSafeEqual(Buffer.from(msg.token), Buffer.from(this.#token))` guarded with a length pre-check (to avoid the `timingSafeEqual` length-must-match requirement throwing). `randomBytes` is already imported from `node:crypto` in that file.

---

### Android — `android/`

#### A-1 · WPA confidence below PolicyGate floor — unreachable alert path

**Files:** `android/core/src/main/kotlin/com/wscanplus/core/threat/WepOpenHeuristic.kt` line 15; `android/core/src/main/kotlin/com/wscanplus/core/threat/PolicyGate.kt` line 4

`SecurityType.WPA → 0.2f` but `PolicyGate` default `minimumConfidence = 0.3f`. Since `0.2f < 0.3f`, every WPA signal is silently discarded — the code path is unreachable and misleading.

**Fix (choose one):**
- (a) Raise WPA confidence to `0.3f` minimum so it can pass the gate (matching the stated intent that WPA is a notable but low-priority warning), or
- (b) Document it explicitly as intentionally suppressed with a `// intentionally below gate floor` comment.

Do not simply raise without a product decision — pick the correct option and implement accordingly.

---

#### A-2 · `ScanMapActivity.loadHeatmap()` is currently unreachable local-renderer code

**File:** `android/app/src/main/kotlin/com/wscanplus/app/ScanMapActivity.kt` lines 58–79 (`onCreate`), line 119 (`loadHeatmap`)

`onCreate()` sets a static "Google Maps required" status message. The private `loadHeatmap()`/`renderHeatmap()` local overlay path is not currently wired into lifecycle calls.

**Fix:** Treat this as cleanup under the current Google-Maps-only decision. Prefer documenting/suppressing the dormant local-renderer path (or removing it in a dedicated cleanup issue) rather than re-enabling a non-Google map flow by accident.

---

#### A-3 · `lastGeminiAnalysisAtMs` cross-thread visibility — missing `@Volatile`

**File:** `android/app/src/main/kotlin/com/wscanplus/app/WatchdogService.kt` line 139

`private var lastGeminiAnalysisAtMs: Long = 0L` is written inside `serviceScope.launch { }` (which runs on `Dispatchers.IO`) and read on the `wscanplus-watchdog` executor thread in the scan-results callback. Without `@Volatile`, the executor thread may read a stale zero, allowing Gemini to fire before the cooldown.

**Fix:** Add `@Volatile`.

---

#### A-4 · `degradedMode` cross-thread visibility — missing `@Volatile`

**File:** `android/app/src/main/kotlin/com/wscanplus/app/WatchdogService.kt` line 118

`private var degradedMode = false` is set in `onStartCommand()` on the main thread and read in the `startFuture` executor task (`if (!degradedMode)`) on the `wscanplus-watchdog` thread.

**Fix:** Add `@Volatile`.

---

#### A-5 · `helloClientSocket` / `helloServerSocket` cross-thread — missing `@Volatile`

**File:** `android/app/src/main/kotlin/com/wscanplus/app/WatchdogService.kt` lines 136–137

Both fields are written from the `Dispatchers.IO` coroutine and read/written from the main thread in `closeHelloSockets()` (called from `onDestroy()`).

**Fix:** Add `@Volatile` to both declarations.

---

#### A-6 · `ScanHistoryActivity` and `ThreatResultsActivity` styling/insets: treat as targeted UX cleanup

**Files:**
- `android/app/src/main/kotlin/com/wscanplus/app/ScanHistoryActivity.kt` lines 28–44
- `android/app/src/main/kotlin/com/wscanplus/app/ThreatResultsActivity.kt` lines 39–67

Some activities use `WscanUi.shell()/header()`, while others already use alternate root/inset patterns. These two screens can still be improved for consistency/insets, but this is better framed as targeted UX hardening than as a universal-rule bug.

**Fix:** If prioritized, align these screens with the preferred shell/header/inset pattern in a focused UI cleanup PR and validate on gesture-nav/cutout devices.

---

## Reference Files

```
desktop/main.js
desktop/scanner.mjs
desktop/store.mjs
desktop/renderer.mjs
desktop/companionServer.mjs
desktop/adbPreflight.mjs
desktop/adbPreflight.test.js
android/app/src/main/kotlin/com/wscanplus/app/WatchdogService.kt
android/app/src/main/kotlin/com/wscanplus/app/ScanHistoryActivity.kt
android/app/src/main/kotlin/com/wscanplus/app/ThreatResultsActivity.kt
android/app/src/main/kotlin/com/wscanplus/app/ScanMapActivity.kt
android/app/src/main/kotlin/com/wscanplus/app/WscanUi.kt
android/core/src/main/kotlin/com/wscanplus/core/threat/WepOpenHeuristic.kt
android/core/src/main/kotlin/com/wscanplus/core/threat/PolicyGate.kt
```

## Verification Commands for follow-up fix PRs

```sh
# Desktop
cd desktop && npm test && npm run lint

# Android
cd android && ./gradlew :core:test :app:ktlintCheck :core:ktlintCheck :app:assembleDebug :app:assembleRelease
```
