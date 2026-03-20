# WSCAN+ Dev Build (ADB Check)

**Date**: 2026-03-19 (America/Los_Angeles)

## What Was Checked
Basic package metadata and app launch via ADB, plus logcat filtering for `com.wscanplus.app` strings.

## Package Info (verified)
From `adb shell dumpsys package com.wscanplus.app`:
- `versionName=0.0.1`
- `versionCode=1`
- `targetSdk=36`
- `firstInstallTime=2026-03-19 05:20:39`
- `lastUpdateTime=2026-03-19 05:20:39`

## Launch Check (verified)
Command used:
```powershell
adb shell monkey -p com.wscanplus.app -c android.intent.category.LAUNCHER 1
```
Observed in logcat:
- Activity start: `com.wscanplus.app/.MainActivity`
- Process already running in background
- App displayed successfully

## Logcat Findings
Filtered logcat for `wscanplus|WSCAN|com.wscanplus.app` after launch.
- Only system/ActivityTaskManager entries referencing the package were seen.
- **No app?emitted logs** were detected in this pass.

## Implication
To validate BLE or altitude signals from the dev build, we need app?side logging (explicit tags) or a debug UI that surfaces captured data.
