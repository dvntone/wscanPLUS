# Handoff: wscan+ Code Review Fixes

**Repo:** `dvntone/wscanplus`
**Branch:** `copilot/fix-review-app-code-inconsistencies`
**Date:** 2026-05-02
**Prepared by:** ChatGPT code review session

---

## Completed in this branch

- Renamed the misspelled `PRELIGHT_CLASSIFICATIONS` constant to `PREFLIGHT_CLASSIFICATIONS` across source, main-process imports, and tests. No compatibility alias remains.
- Added a scan-generation guard in `desktop/scanner.mjs` that discards in-flight scan results **before** store mutation after `stopScanning()`.
- Hardened `desktop/companionServer.mjs` token auth with length-checked `crypto.timingSafeEqual()`.
- Formatted `CompanionServer.address` as a displayable string instead of returning raw `AddressInfo` objects.
- Fixed `desktop/renderer.mjs` risk-log handling so emitted snapshots replace/dedupe local risk state and render once, instead of appending every snapshot entry repeatedly.
- Raised WPA heuristic confidence from `0.2f` to `0.3f` so the WPA warning path can actually pass the default `PolicyGate` floor.
- Wired `ScanMapActivity.loadHeatmap()` from `onStart()` instead of marking implemented code unused.
- Refactored `ScanHistoryActivity` and `ThreatResultsActivity` onto the shared `WscanUi.shell()` / `WscanUi.header()` path so they inherit the same window preparation and system-inset handling as `DiagnosticActivity`.

---

## Additional review findings appended

These findings were confirmed during independent review and must not be bypassed with suppressions or compatibility shims.

### R-1 · Scanner stop race must be fixed before store mutation

A generation check in `.finally()` is too late because `runScanAndProcess()` mutates `store.addAps()` / `store.addRiskEntries()` before the promise finalizer. The fix in this branch checks `store.state.scanning` and the generation value immediately after parsing scan output and before any store write.

### R-2 · Risk-log renderer path duplicated snapshot entries

`store.addRiskEntries()` emits the full `riskLog` snapshot. The renderer previously looped through that snapshot and appended each item through `addRisk()`, duplicating existing risk rows and triggering repeated renders. This branch now treats the payload as a snapshot, normalizes/dedupes into `state.risks`, updates AP risk state, emits new detector events deliberately, and calls `render()` once.

### R-3 · `loadHeatmap()` must not be suppressed as unused

The heatmap implementation is functional and backed by stored GPS-tagged scan rows. The proper fix is wiring it into lifecycle. This branch wires it from `onStart()`.

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
