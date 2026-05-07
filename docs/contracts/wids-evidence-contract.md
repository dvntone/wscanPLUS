# wscan+ WIDS Evidence Contract

Issue: #294
Status: Phase 6 PR 1 scope — documentation and fixtures only

## Purpose

This contract defines the normalized evidence shape used by future wscan+ WIDS work across Android, desktop, and web UI surfaces.

This PR intentionally changes no runtime code. It adds the platform-neutral contract and representative fixtures that later implementation PRs must conform to.

## Principles

- The repo-level contract is the source of truth.
- Android, desktop, and web UI should adapt to this shape rather than inventing separate alert models.
- Existing Android heuristics must be wrapped into this model later, not replaced.
- Desktop detector output must be normalized into this model later, not duplicated.
- Android normal WiFi scanning is not monitor-mode packet capture.
- Android-only detections must carry source limitations and confidence caps.
- Recurring radio signatures are correlation evidence, not attribution evidence.

## Source types

```json
[
  "android_wifi",
  "android_ble",
  "android_sensor",
  "desktop_wifi",
  "linux_monitor",
  "adb",
  "manual"
]
```

## Observation object

```json
{
  "id": "obs_001",
  "schemaVersion": 1,
  "source": "android_wifi",
  "sourceDeviceId": "android_pixel_001",
  "timestamp": 1777990000000,
  "sessionId": "session_001",
  "ssid": "TrustedNet",
  "bssid": "aa:bb:cc:dd:ee:ff",
  "frequencyMhz": 2412,
  "channel": 1,
  "rssiDbm": -54,
  "capabilities": "[WPA2-PSK-CCMP][ESS]",
  "oui": "Example Networks",
  "bleAddress": null,
  "bleName": null,
  "bleRssiDbm": null,
  "motionState": "still",
  "magneticMagnitude": null,
  "floorEstimate": null,
  "scanAgeMs": 850,
  "cachedOrThrottled": "unknown",
  "zoneLabel": "home-office"
}
```

## Alert object

```json
{
  "id": "alert_001",
  "schemaVersion": 1,
  "type": "suspected_evil_twin",
  "severity": "high",
  "confidence": 0.75,
  "title": "Suspected Evil Twin",
  "explanation": "Known SSID was observed from an unfamiliar BSSID with changed security capabilities and abnormal RSSI behavior.",
  "evidence": [],
  "limitations": [],
  "recommendedAction": "Avoid connecting until the AP is verified against the trusted router inventory.",
  "createdAt": 1777990001000,
  "sourceDeviceIds": ["android_pixel_001"],
  "detectorVersion": "phase6-wids-v1"
}
```

## Evidence item

```json
{
  "kind": "unknown_bssid",
  "weight": 0.3,
  "description": "SSID matches a trusted network, but BSSID is not present in the trusted baseline.",
  "observationIds": ["obs_001"]
}
```

## Alert types

- `trusted_network_drift`
- `suspected_evil_twin`
- `suspicious_rogue_ap`
- `possible_deauth`
- `management_frame_dos`
- `recurring_radio_signature`
- `baseline_deviation`
- `android_scan_limited`

## Evidence kinds

- `ssid_match`
- `unknown_bssid`
- `security_change`
- `channel_change`
- `rssi_anomaly`
- `multi_sensor_match`
- `monitor_frame_event`
- `ble_correlation`
- `walk_test_inconsistency`
- `baseline_missing`
- `scan_throttling_possible`
- `floor_or_zone_mismatch`
- `recurrence`

## Confidence caps

These are contract rules for later implementation, not runtime code in this PR.

| Scenario | Max confidence without stronger evidence |
| --- | ---: |
| Android-only suspected evil twin | 0.75 |
| Android-only possible deauth-like disruption | 0.45 |
| Android-only recurring radio signature | 0.65 |

Monitor-mode, hardware-backed, or user-confirmed evidence may lift the cap if the evidence item explicitly records that stronger source.

## Required limitations language

Android-only WiFi alerts should include:

```text
Android companion mode provides environmental WiFi scan evidence, not monitor-mode packet capture.
```

Android-only possible deauth alerts should include:

```text
Normal Android WiFi APIs do not expose raw management frames; this alert is based on indirect scan/connectivity behavior and needs monitor-mode confirmation.
```

Recurring radio signature alerts should include:

```text
This identifies recurring radio observations, not a person, owner, or intent.
```

## Fixture files

Representative fixtures live under:

```text
docs/contracts/fixtures/wids/
```

Required fixture coverage:

- trusted network drift;
- suspected evil twin;
- Android-only possible deauth capped at 0.45;
- monitor-mode management-frame DoS;
- recurring radio signature;
- baseline deviation.
