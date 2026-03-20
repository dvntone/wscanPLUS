# ADB Expanded Findings (Non-Play-Store Context)

Created: 2026-03-19
Device: <redacted-device>
Scope: Read-only adb checks with non-Play-Store assumption

## Commands Run

- adb devices
- adb shell getprop ro.build.version.sdk
- adb shell getprop ro.build.version.release
- adb shell dumpsys wifi
- adb shell dumpsys connectivity

## Verified Results

Device and OS
- Device connected: <redacted-device>
- Android SDK: 34
- Android release: 14

Wi-Fi Service State (dumpsys wifi)
- Wi-Fi enabled
- ScanAlwaysAvailable: true
- Periodic scan timer interval: 20000 ms
- Firmware roaming supported (no partial scan)
- Scan result summary (last log snapshot)
  - totalScanResults: 91
  - hiddenNetworkScanResults: 30
  - openNetworkScanResults: 6
  - legacyPersonalNetworkScanResults: 49
  - legacyEnterpriseNetworkScanResults: 17
  - wpa3PersonalNetworkScanResults: 19

Connectivity State (dumpsys connectivity)
- Active default network: Wi-Fi (network id 119)
- Transport: WIFI
- NetworkCapabilities: NOT_METERED, INTERNET, NOT_RESTRICTED, TRUSTED, NOT_VPN, VALIDATED, NOT_ROAMING
- Link info: SSID "<redacted-ssid>", BSSID <redacted-bssid>, RSSI -52, frequency 5785 MHz

## What Produced Data

- Wi-Fi scan stats and scheduling come from dumpsys wifi
- Network transport, capabilities, and redacted SSID/BSSID fields come from dumpsys connectivity

## What Requires More Research (Non-Play-Store OK)

- READ_LOGS access path for wpa_supplicant reason codes and disassociation/deauth reasons
- Device-specific vendor logs and permissions required for access

## Notes

- All commands were read-only
- Output reflects current device state only
- This document assumes non-Play-Store policy per docs/research/codex/10_non_playstore_policy.md

