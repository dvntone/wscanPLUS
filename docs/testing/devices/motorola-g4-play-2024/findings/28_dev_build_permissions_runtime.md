# WSCAN+ Dev Build Runtime & Permissions (ADB)

**Date**: 2026-03-19 (America/Los_Angeles)

## Purpose
Capture what the dev build is currently allowed to do (permissions + appops) and whether its foreground service is running.

## Requested & Granted Permissions (verified)
From `adb shell dumpsys package com.wscanplus.app`:
- Requested:
  - ACCESS_WIFI_STATE
  - CHANGE_WIFI_STATE
  - ACCESS_FINE_LOCATION
  - ACCESS_COARSE_LOCATION
  - NEARBY_WIFI_DEVICES
  - FOREGROUND_SERVICE
  - FOREGROUND_SERVICE_DATA_SYNC
  - INTERNET
  - USE_BIOMETRIC
  - ACCESS_NETWORK_STATE
- Runtime granted:
  - ACCESS_FINE_LOCATION: granted=true
  - ACCESS_COARSE_LOCATION: granted=true
  - NEARBY_WIFI_DEVICES: granted=true

## AppOps (verified)
From `adb shell cmd appops get com.wscanplus.app`:
- COARSE_LOCATION: allow
- FINE_LOCATION: allow (recent rejectTime recorded)
- RUN_ANY_IN_BACKGROUND: allow
- START_FOREGROUND: allow (running)
- ACCESS_RESTRICTED_SETTINGS: allow

## Foreground Service (verified)
From `adb shell dumpsys activity services com.wscanplus.app`:
- `com.wscanplus.app/.WatchdogService` is **foreground**, with notification channel `wscanplus_watchdog`.

## Implication
- The app already has the runtime permissions needed for Wi?Fi scanning and location signals.
- A foreground service is running; adding BLE/location logging can be done without new runtime prompts.
