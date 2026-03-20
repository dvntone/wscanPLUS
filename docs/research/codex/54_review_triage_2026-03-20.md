# 2026-03-20 Review Triage Notes

## Purpose

This note records a cross-repo review pass performed by Codex on March 20, 2026 so future Claude/Codex sessions do not need to rediscover the same context.

Scope:

- `wscanplus` Android repo runtime behavior and repo docs
- companion desktop repo `wscanplus_desktop` for Electron hardening notes

Method:

- local code review of the checked-out repositories
- verification against repo handoff files and recent merged commits
- external confirmation against primary sources only

Primary sources used:

- Android Wi-Fi scanning overview: [developer.android.com/develop/connectivity/wifi/wifi-scan](https://developer.android.com/develop/connectivity/wifi/wifi-scan)
- Android `WifiManager` reference: [developer.android.com/reference/android/net/wifi/WifiManager](https://developer.android.com/reference/android/net/wifi/WifiManager)
- Gradle Wrapper user guide: [docs.gradle.org/current/userguide/gradle_wrapper.html](https://docs.gradle.org/current/userguide/gradle_wrapper.html)
- Windows command quoting guidance: [learn.microsoft.com/en-us/windows-server/administration/windows-commands/cmd](https://learn.microsoft.com/en-us/windows-server/administration/windows-commands/cmd)
- Windows long paths with spaces guidance: [learn.microsoft.com/en-us/troubleshoot/windows-server/setup-upgrade-and-drivers/filenames-with-spaces-require-quotation-mark](https://learn.microsoft.com/en-us/troubleshoot/windows-server/setup-upgrade-and-drivers/filenames-with-spaces-require-quotation-mark)
- Electron security checklist: [electronjs.org/docs/latest/tutorial/security](https://www.electronjs.org/docs/latest/tutorial/security)
- Electron process sandboxing: [electronjs.org/docs/latest/tutorial/sandbox](https://www.electronjs.org/docs/latest/tutorial/sandbox)

## Findings

### A1. Android standard scanner has an API 28-29 scan-initiation gap

Who:

- Android `StandardScanner`
- any future agent touching scanner behavior, scan reliability, or Android 9/10 support claims

What:

- The current implementation listens for scan result broadcasts on API 24-29, but only initiates a scan on API `< 28`.
- On API 28-29 specifically, there is no in-app scan trigger in the current code path.

Where:

- [StandardScanner.kt](/Users/Devia/Documents/GitHub/wscanplus/android/core/src/main/kotlin/com/wscanplus/core/scanner/StandardScanner.kt)
- `startWithBroadcastReceiver()` registers `SCAN_RESULTS_AVAILABLE_ACTION`
- `wifiManager.startScan()` is guarded behind `Build.VERSION.SDK_INT < Build.VERSION_CODES.P`

Why this matters:

- Android still documents the classic `startScan()` -> listen for `SCAN_RESULTS_AVAILABLE_ACTION` -> call `getScanResults()` flow, while also marking `startScan()` deprecated from API 28 onward and throttled on modern Android.
- This repo contains no alternate API 28-29 scan trigger.
- That behavior is not called out in `KNOWN_ISSUES.md`, so a future agent could incorrectly assume API 28-29 is an actively-driven baseline path.

Repo-doc status:

- Partially reflected in [docs/SESSION_STATE.md](/Users/Devia/Documents/GitHub/wscanplus/docs/SESSION_STATE.md): the current project memory explicitly says `startScan()` is only used on API `< 28`.
- Not recorded as a known limitation or product decision in [KNOWN_ISSUES.md](/Users/Devia/Documents/GitHub/wscanplus/KNOWN_ISSUES.md).

Direct evidence:

- `startWithBroadcastReceiver()` is the active code path for API 24-29.
- `wifiManager.startScan()` is only called when `SDK_INT < P`.
- A repo-wide search on March 20, 2026 found no other Android call site for `startScan()`.

Decision required:

- The repo needs an explicit compatibility stance for Android 9/10:
  - either passive-only scan ingestion is accepted and documented, or
  - API 28-29 must be given an active trigger path and verified on-device

Recommended next step:

- Record the compatibility decision first.
- If active scanning is required on API 28-29, back the change with device verification and update both `SESSION_STATE` and `KNOWN_ISSUES`.

Priority:

- `P1 decision item`

### A2. Windows `gradlew.bat` is fragile when `JAVA_HOME` contains spaces

Who:

- Windows contributors and agents running Android verification locally
- any future agent relying on `gradlew.bat` for required checks

What:

- The checked-in Windows wrapper builds `JAVA_EXE` from `%JAVA_HOME%/bin/java.exe` and invokes `%JAVA_EXE%` without surrounding quotes.

Where:

- [gradlew.bat](/Users/Devia/Documents/GitHub/wscanplus/android/gradlew.bat)

Why this matters:

- Windows command execution requires quoting paths that contain spaces.
- During local verification on March 20, 2026, the wrapper failed before Gradle startup in an environment where `JAVA_HOME` pointed to `C:\Users\Devia\AppData\Local\Programs\Eclipse Adoptium\...`.
- That blocks one of the repo's required verification commands on Windows even though Java is installed.

Repo-doc status:

- Not documented in [KNOWN_ISSUES.md](/Users/Devia/Documents/GitHub/wscanplus/KNOWN_ISSUES.md).
- Not reflected in the current handoff, which notes successful wrapper-based verification in a prior environment.

Assessment:

- Small, isolated build-tooling defect.
- Does not change product behavior, but it weakens local verification reliability.

Status update:

- Resolved in the local worktree on March 20, 2026 by quoting `%JAVA_EXE%` and switching the constructed Windows path to `\bin\java.exe`.

Verification after fix:

- `cmd /c gradlew.bat :core:test` passed
- `cmd /c gradlew.bat :core:ktlintCheck :app:ktlintCheck` passed
- `git ls-files .env android/local.properties` returned no tracked files

Priority:

- `Resolved during triage`

### A3. `StandardScanner.stop()` still swallows teardown errors without logging

Who:

- Android scanner lifecycle and future diagnostics work
- any future agent following repo policy in `docs/SECURITY.md`

What:

- The receiver unregister path and callback unregister path catch exceptions and intentionally do not log them.

Where:

- [StandardScanner.kt](/Users/Devia/Documents/GitHub/wscanplus/android/core/src/main/kotlin/com/wscanplus/core/scanner/StandardScanner.kt)

Why this matters:

- The repo security/process rules explicitly say caught exceptions must be logged and not silently swallowed.
- The current comments defer logging to the future, which means lifecycle mismatches on real devices become harder to diagnose.

Repo-doc status:

- Not listed as a known issue.
- Directly conflicts with [docs/SECURITY.md](/Users/Devia/Documents/GitHub/wscanplus/docs/SECURITY.md).

Assessment:

- Small correctness/observability fix.
- Good candidate for a narrow follow-up PR after the wrapper fix.

Status update:

- Resolved in the local worktree on March 20, 2026 by adding `Log.w(...)` in both teardown catch paths.

Verification after fix:

- `cmd /c gradlew.bat :core:test` passed
- `cmd /c gradlew.bat :core:ktlintCheck :app:ktlintCheck` passed

Priority:

- `Resolved during triage`

## Related desktop note

Electron-specific hardening findings for the companion desktop repo are recorded separately in:

- [docs/REVIEW_TRIAGE_2026-03-20.md](/Users/Devia/Documents/GitHub/wscanplus_desktop/docs/REVIEW_TRIAGE_2026-03-20.md)

The key desktop item is missing explicit `sandbox: true` plus missing CSP for the current local renderer shell.

## Recommended fix order

1. Triage the API 28-29 scan behavior as either an intentional passive-only path or a compatibility bug.
2. Address the companion desktop Electron hardening gap in the desktop repo.
