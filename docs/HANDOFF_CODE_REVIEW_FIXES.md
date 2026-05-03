# Handoff: wscan+ Code Review Fixes

**Repo:** `dvntone/wscanplus`
**Branch:** `copilot/fix-review-app-code-inconsistencies`
**Date:** 2026-05-02
**Prepared by:** ChatGPT code review session

---

## Completed in this branch

- Fixed the misspelled `PRELIGHT_CLASSIFICATIONS` source constant by introducing `PREFLIGHT_CLASSIFICATIONS` in `desktop/adbPreflight.mjs`.
- Added a temporary compatibility export for the old spelling so existing import sites remain functional until dependent files are safely updated.
- Added a scan-generation guard in `desktop/scanner.mjs` that discards in-flight scan results **before** store mutation after `stopScanning()`.
- Hardened `desktop/companionServer.mjs` token auth with length-checked `crypto.timingSafeEqual()`.
- Formatted `CompanionServer.address` as a displayable string instead of returning raw `AddressInfo` objects.
- Raised WPA heuristic confidence from `0.2f` to `0.3f` so the WPA warning path can actually pass the default `PolicyGate` floor.
- Wired `ScanMapActivity.loadHeatmap()` from `onStart()` instead of marking implemented code unused.

---

## Additional review findings appended

These findings were confirmed during independent review and must not be bypassed with suppressions or compatibility shims.

### R-1 · Scanner stop race must be fixed before store mutation

A generation check in `.finally()` is too late because `runScanAndProcess()` mutates `store.addAps()` / `store.addRiskEntries()` before the promise finalizer. The fix in this branch checks `store.state.scanning` and the generation value immediately after parsing scan output and before any store write.

### R-2 · Risk-log renderer path likely duplicates snapshot entries

`store.addRiskEntries()` emits the full `riskLog` snapshot. The renderer currently loops through that snapshot and appends each item through `addRisk()`, which can duplicate existing risk rows and trigger repeated renders. Proper fix: treat the payload as a snapshot, normalize/dedupe into `state.risks`, update AP risk state, emit any new event rows deliberately, and call `render()` once.

### R-3 · `loadHeatmap()` must not be suppressed as unused

The heatmap implementation is functional and backed by stored GPS-tagged scan rows. The proper fix is wiring it into lifecycle. This branch wires it from `onStart()`.

### R-4 · WPA confidence mismatch is a product behavior bug, not only dead code

If WPA should be a low-priority warning, it must meet the current `PolicyGate` floor. This branch raises WPA confidence to `0.3f` rather than adding a comment that hides the unreachable path.

### R-5 · Temporary preflight compatibility alias must be removed in final cleanup

The branch currently keeps `PRELIGHT_CLASSIFICATIONS` as an alias to avoid breaking imports while connector-only edits are limited. Final cleanup should update `desktop/main.js` and `desktop/adbPreflight.test.js` to import/use `PREFLIGHT_CLASSIFICATIONS`, then remove the alias.

---

## Remaining required fixes before ready-for-review

- Update `desktop/main.js` and `desktop/adbPreflight.test.js` to use `PREFLIGHT_CLASSIFICATIONS`; remove the compatibility alias from `desktop/adbPreflight.mjs`.
- Fix `desktop/renderer.mjs` risk-log snapshot handling without appending duplicates and without N full renders.
- Add `@Volatile` to `WatchdogService.kt` fields crossing main/executor/IO threads:
  - `degradedMode`
  - `helloServerSocket`
  - `helloClientSocket`
  - `lastGeminiAnalysisAtMs`
- Refactor `ScanHistoryActivity` and `ThreatResultsActivity` to use `WscanUi.shell()`, `WscanUi.header()`, `WscanUi.prepareWindow()`, and `WscanUi.applySystemInsets()` following the `DiagnosticActivity` pattern.
- Run required checks:
  - `cd desktop && npm test && npm run lint`
  - `cd android && ./gradlew :core:test :app:ktlintCheck :core:ktlintCheck`

---

## No-bypass rule

Do not mark this PR ready by adding `@Suppress("unused")`, retaining compatibility aliases as final fixes, weakening tests, skipping CI, or documenting unreachable code without a product decision. Every item above needs a real implementation fix.
