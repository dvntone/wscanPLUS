# WSCAN+ Dev Build Components (ADB)

**Date**: 2026-03-19 (America/Los_Angeles)

## Purpose
Capture declared activities/receivers/providers visible via `dumpsys package` for the dev build.

## Activities (verified)
- `com.wscanplus.app/.MainActivity` (LAUNCHER)

## Receivers (verified)
- `androidx.profileinstaller.ProfileInstallReceiver`
  - Actions: SAVE_PROFILE, INSTALL_PROFILE, SKIP_FILE, BENCHMARK_OPERATION

## Providers (verified)
- `com.google.firebase.provider.FirebaseInitProvider`
- `androidx.startup.InitializationProvider`

## Notes
The `dumpsys package` output does **not** list app?specific services in the resolver table, but we confirmed `com.wscanplus.app/.WatchdogService` is running as a foreground service in `28_dev_build_permissions_runtime.md`.
