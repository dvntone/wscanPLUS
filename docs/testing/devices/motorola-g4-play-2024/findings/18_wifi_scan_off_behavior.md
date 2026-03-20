# Wi-Fi Off + Scan-Always Verification (ADB)

**Date**: 2026-03-19 (America/Los_Angeles)

## Purpose
Verify whether scan results are available when Wi?Fi is turned **off**, and how behavior changes based on the OS setting **wifi_scan_always_enabled**.

## Test 1: Wi?Fi OFF + scan?always OFF (no results)
**Commands**:

```powershell
adb shell cmd wifi status
adb shell settings get global wifi_on
adb shell settings get global wifi_scan_always_enabled
adb shell cmd wifi list-scan-results
```

**Observed**:
- `wifi_on = 0`
- `wifi_scan_always_enabled = 0`
- `cmd wifi list-scan-results` ? **No scan results**

**Implication**:
- Without scan?always, the OS will not return scan results when Wi?Fi is off.

## Test 2: Enable scan?always, then Wi?Fi OFF (results available)
**Enable scan?always**:

```powershell
adb shell settings put global wifi_scan_always_enabled 1
adb shell settings get global wifi_scan_always_enabled
```

**Commands (Wi?Fi OFF)**:

```powershell
adb shell cmd wifi status
adb shell settings get global wifi_on
adb shell settings get global wifi_scan_always_enabled
adb shell cmd wifi list-scan-results
```

**Observed at 2026?03?19 08:04:05**:
- `wifi_on = 0`
- `wifi_scan_always_enabled = 1`
- `cmd wifi list-scan-results` ? **Populated list returned**

**Implication**:
- With scan?always enabled, scan results are available even while Wi?Fi is off.

## Notes for Integration
- The Android companion app can conditionally rely on scans with Wi?Fi off **only if** `wifi_scan_always_enabled = 1`.
- The desktop/ADB tool can check and optionally set this flag when user consent and policy allow.
- If scan?always is disabled, the system must either prompt to enable it or switch to a passive/reactive mode.

## Security/Privacy
The scan results include SSIDs/BSSIDs and should be treated as sensitive data.
