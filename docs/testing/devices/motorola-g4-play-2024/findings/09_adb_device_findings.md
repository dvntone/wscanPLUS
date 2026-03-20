# ADB Device Findings (Read-Only)

Created: 2026-03-19
Device: <redacted-device>
Scope: Read-only adb checks for context

## Commands Run

- adb devices
- adb shell getprop ro.build.version.sdk
- adb shell getprop ro.build.version.release
- adb shell dumpsys wifi

## Verified Results

Device and OS
- Device listed as connected: <redacted-device>
- Android SDK: 34
- Android release: 14

Wi-Fi State (from dumpsys wifi)
- Wi-Fi enabled
- ScanAlwaysAvailable: true
- Periodic scan timer interval: 20000 ms
- Firmware roaming supported (no partial scan)
- mWifiLogProto.numTotalScanResults: 91
- mWifiLogProto.numHiddenNetworkScanResults: 30
- mWifiLogProto.numOpenNetworkScanResults: 6
- mWifiLogProto.numLegacyPersonalNetworkScanResults: 49
- mWifiLogProto.numLegacyEnterpriseNetworkScanResults: 17
- mWifiLogProto.numWpa3PersonalNetworkScanResults: 19

## What Produced Data

- getprop values: OS version and SDK
- dumpsys wifi: current Wi-Fi service state, scan scheduling, and scan result summary

## What Still Needs Research / Privileged Access

- Supplicant reason codes and deauth reasons are not exposed via supported public APIs
- ROADMAP mentions READ_LOGS and other privileged paths for deauth detection, which are not Play Store compatible

See: docs/ROADMAP.md (Deferred: READ_LOGS / wpa_supplicant reason codes)

## Notes

- All commands were read-only
- Data here is device-specific and should not be generalized

