# Handoff: wscan+ Code Review Fixes

**Repo:** `dvntone/wscanplus`
**Branch:** `copilot/fix-review-app-code-inconsistencies`
**Date:** 2026-05-02
**Traceability:** PR #280 / issue #279 review-fix handoff for Claude/Copilot continuation

---

## Completed in this branch

- Renamed the misspelled `PRELIGHT_CLASSIFICATIONS` constant to `PREFLIGHT_CLASSIFICATIONS` across source, main-process imports, and tests. No compatibility alias remains.
- Added a scan-generation guard in `desktop/scanner.mjs` that discards stale continuous-loop scan results **before** store mutation after `stopScanning()`, while keeping manual idle scans functional.
- Hardened `desktop/companionServer.mjs` token auth with length-checked `crypto.timingSafeEqual()`.
- Formatted `CompanionServer.address` as a displayable string and updated the pairing UI to accept formatted address strings.
- Fixed `desktop/renderer.mjs` risk-log handling so emitted snapshots replace/dedupe local risk state and render once, instead of appending every snapshot entry repeatedly.
- Raised WPA heuristic confidence from `0.2f` to `0.3f` so the WPA warning path can actually pass the default `PolicyGate` floor. The matching unit test already expects `0.3f` on this branch.
- Preserved the locked Android mapping decision: `ScanMapActivity` continues to show the Google Maps-required status instead of re-enabling the custom `LocalHeatmapView` path without a product-doc update.
- Refactored `ScanHistoryActivity` and `ThreatResultsActivity` onto the shared `WscanUi.shell()` / `WscanUi.header()` path so they inherit the same window preparation and system-inset handling as `DiagnosticActivity`.

---

## Additional review findings appended

These findings were confirmed during independent review and must not be bypassed with suppressions or compatibility shims.

### R-1 · Scanner stop race must be fixed before store mutation

A generation check in `.finally()` is too late because `runScanAndProcess()` mutates `store.addAps()` / `store.addRiskEntries()` before the promise finalizer. The fix in this branch checks the scan generation immediately after parsing scan output and before any continuous-loop store write. Manual scans do not require `store.state.scanning`, so the idle-state “Scan Now” path remains usable.

### R-2 · Risk-log renderer path duplicated snapshot entries

`store.addRiskEntries()` emits the full `riskLog` snapshot. The renderer previously looped through that snapshot and appended each item through `addRisk()`, duplicating existing risk rows and triggering repeated renders. This branch now treats the payload as a snapshot, normalizes/dedupes into `state.risks`, updates AP risk state, emits new detector events deliberately, and calls `render()` once.

### R-3 · `loadHeatmap()` must not be suppressed or re-enabled against locked docs

The local heatmap implementation exists, but Android mapping is currently locked to Google Maps in the authoritative docs. The correct fix for this PR is to keep the custom local heatmap path disabled unless a separate product decision updates the mapping docs.

### R-4 · WPA confidence mismatch is a product behavior bug, not only dead code

If WPA should be a low-priority warning, it must meet the current `PolicyGate` floor. This branch raises WPA confidence to `0.3f` rather than adding a comment that hides the unreachable path.

---

## Remaining required fixes before ready-for-review

- Add `@Volatile` to `WatchdogService.kt` fields crossing main/executor/IO threads:
  - `degradedMode`
  - `helloServerSocket`
  - `helloClientSocket`
  - `lastGeminiAnalysisAtMs`
- Run required checks:
  - `cd desktop && npm test && npm run lint`
  - `cd android && ./gradlew :core:test :app:ktlintCheck :core:ktlintCheck`

---

## No-bypass rule

Do not mark this PR ready by adding `@Suppress("unused")`, weakening tests, skipping CI, or documenting unreachable code without a product decision. Every item above needs a real implementation fix.
