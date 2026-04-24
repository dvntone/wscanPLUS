# Scan Reliability Matrix

Status: implementation/test planning  
Related: #234

This matrix defines the required validation path before expanding the spatial baseline MVP beyond basic collection and local comparison.

The goal is to determine how Android scan behavior changes across connectivity state, foreground service state, screen/lock state, battery optimization, Location Services, Android version, and OEM behavior.

## Why this exists

wscan+ must not assume that a missing observation means a safe environment. Missing observations may be caused by throttling, stale scan results, disabled Location Services, denied permissions, foreground-service failure, OEM task killing, or battery optimization.

Every scan cycle and test report must distinguish:

```text
fresh observation
stale observation
permission blocked
system disabled
radio disabled
battery restricted
foreground service inactive
OEM/background restricted
unknown / untested
```

## Test axes

Run each device through the combinations that are practical for that device.

### Connectivity

```text
Wi-Fi connected
Wi-Fi disconnected
mobile data on
mobile data off
no internet
airplane mode with Wi-Fi re-enabled
hotspot/tethering active
USB tethering active
```

### Screen / lifecycle

```text
screen on
screen off
lock screen visible
secure lock screen
app foreground
app backgrounded with HOME
app removed from recents
foreground service on
foreground service off
persistent notification visible
persistent notification dismissed if allowed
```

### System settings

```text
Location Services on
Location Services off
Bluetooth on
Bluetooth off
Wi-Fi scan always available setting where exposed
battery optimization enabled
battery optimization ignored/disabled for app
Do Not Disturb on/off
Low Power mode / Battery Saver on/off
```

### Android versions

```text
Android 10
Android 11
Android 12
Android 13
Android 14
Android 15
Android 16 / current target path where available
```

### OEM / device families

```text
Pixel
Motorola
Samsung
OnePlus/Xiaomi if available
Revvl/T-Mobile tablet path
older low-end Android collector device
```

## Required fields per test run

Each run should produce a structured report with at least:

```text
device model
manufacturer
Android version
API level
app version
build variant
test case name
test start timestamp
test duration
permissions granted
Location Services state
Bluetooth state
Wi-Fi state
internet connectivity state
battery optimization state
foreground service state
notification state
screen state
lock state
startScan return value
SCAN_RESULTS_AVAILABLE_ACTION received
EXTRA_RESULTS_UPDATED value
newest ScanResult timestamp
fresh/stale classification
Wi-Fi result count
BLE result count
cell result count
readiness blockers
system events generated
operator notes
```

## Example report shape

```json
{
  "schemaVersion": 1,
  "deviceModel": "XT2613-1",
  "manufacturer": "Motorola",
  "androidVersion": "15",
  "apiLevel": 35,
  "appVersion": "0.1.0",
  "testCase": "screen_off_fgs_on_wifi_connected",
  "durationMinutes": 10,
  "permissions": {
    "fineLocation": true,
    "nearbyWifiDevices": true,
    "bluetoothScan": true,
    "bluetoothConnect": true,
    "backgroundLocation": true
  },
  "systemState": {
    "locationServices": true,
    "bluetooth": true,
    "wifiConnected": true,
    "mobileData": false,
    "internetAvailable": true,
    "batteryOptimizationIgnored": true,
    "foregroundServiceRunning": true,
    "notificationVisible": true,
    "screenState": "off",
    "lockState": "secure_locked"
  },
  "wifi": {
    "startScanReturned": true,
    "broadcastReceived": true,
    "platformUpdated": true,
    "freshCount": 18,
    "staleCount": 0,
    "newestResultAgeMs": 2140
  },
  "ble": {
    "scanReady": true,
    "resultCount": 6,
    "staleEvictions": 1
  },
  "cell": {
    "resultCount": 3
  },
  "readinessBlockers": [],
  "notes": "No observed scan loss during 10-minute run"
}
```

## Minimum test cases

### 1. Foreground baseline

```text
App foreground
Screen on
Location Services on
Wi-Fi connected
Bluetooth on
Battery optimization ignored
Foreground service running
```

Expected:

```text
Wi-Fi observations collected
BLE observations collected if hardware supports BLE
No stale warning after initial warmup
No permission blockers
```

### 2. Background with foreground service

```text
App backgrounded with HOME
Screen on or off
Foreground service running
Persistent notification visible
```

Expected:

```text
Collection continues or degraded state is recorded
No silent failure
Readiness state reflects actual collection state
```

### 3. Secure lock screen

```text
App started normally
Foreground service running
Device locked with secure lock screen
Screen off for 10 minutes
```

Expected:

```text
Foreground service survival recorded
Wi-Fi freshness recorded
BLE continuity recorded if enabled
Any loss is captured as system event / blocker
```

### 4. Location Services disabled

```text
Required permissions granted
Location Services disabled at system level
```

Expected:

```text
Readiness includes LOCATION_SERVICES_DISABLED
Wi-Fi scanner does not treat missing results as safe
UI can explain corrective action
```

### 5. Fine location denied

```text
ACCESS_FINE_LOCATION denied
Other nearby permissions granted where possible
```

Expected:

```text
Readiness includes MISSING_FINE_LOCATION
Minimum scan does not start as normal mode
Any degraded mode is explicit
```

### 6. Wi-Fi disconnected but scan allowed

```text
Wi-Fi not connected to AP
Wi-Fi radio on
Location Services on
Permissions granted
```

Expected:

```text
Scan behavior measured
Results are fresh/stale classified
No assumption that connectivity is required unless device proves otherwise
```

### 7. No internet

```text
Wi-Fi disconnected
Mobile data off
No internet route
```

Expected:

```text
Local collection still works where Android permits
Maps/Gemini/CTI unavailable states do not block local scan/baseline/export
```

### 8. Battery optimization enabled

```text
Battery optimization active for app
Foreground service attempted
Screen off/background duration test
```

Expected:

```text
Battery restriction captured in readiness/system events
Collection survival measured
User-facing remediation path documented
```

### 9. BLE permission split

```text
BLUETOOTH_SCAN granted
BLUETOOTH_CONNECT denied
```

Expected:

```text
BLE scan behavior measured
Metadata access degradation recorded
App does not crash on paired metadata paths
```

### 10. Collector-only old device mode

```text
Older device
No SIM
No reliable internet
Local collection only
Deferred export/sync
```

Expected:

```text
Device can still produce local evidence if scan APIs work
UI clearly marks unavailable capabilities
Export remains available
```

## Pass/fail guidance

A test passes when the app correctly identifies and reports the device state, even if Android/OEM behavior prevents collection.

A test fails when:

```text
scan failure is silent
stale results are treated as fresh
permission denial is misclassified
Location Services disabled is not detected
foreground service death is not recorded
missing data is presented as safe
app crashes or hangs
export/report omits relevant readiness blockers
```

## Implementation output

The scan reliability harness should eventually create:

```text
ReliabilityReportEntity
ScanReliabilityRunner
shareable JSON report
human-readable summary
```

Recommended local output path for manual testing:

```text
Android app export directory / wscanplus/reliability/YYYY-MM-DD-device-testcase.json
```

## Relationship to implementation plan

This matrix supports `docs/architecture/android-spatial-baseline-implementation-plan.md`.

The first code phase should implement the shared capability/readiness language before Wi-Fi/BLE collector changes. That lets all later collectors report degraded states consistently.
