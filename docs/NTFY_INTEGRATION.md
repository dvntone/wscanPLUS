# ntfy.sh Integration Guide

This document captures how `ntfy.sh` can be used by wscan+ as an optional alert delivery channel.

The target service is **ntfy.sh**. If a different `nfty.sh` service is intended, verify that before implementation.

---

## Core concept

ntfy publishes notifications with a simple HTTP request:

```bash
curl -d "wscan+ alert body" https://ntfy.sh/<topic>
```

For wscan+, treat the topic as a secret. Do not hard-code predictable topics such as `wscanplus`, `alerts`, or a user name.

Recommended model:

```text
wscan+ event
  -> alert policy
  -> dedup / rate limit
  -> ntfy publisher
  -> configured ntfy endpoint
```

The publisher must be optional and off by default.

---

## Required settings

Minimum settings:

```text
enabled: boolean
serverUrl: https://ntfy.sh
topic: user-generated random topic
minSeverity: high | medium | low | info
authToken: optional
rateLimitWindowSeconds: number
```

Optional settings:

```text
sendTestNotification
includeDeviceName
includeBssid
includeSsid
includeLocationSummary
includeRayhunterSummary
clickUrl
quietHours
```

Storage requirements:

- Store topic/token as sensitive settings.
- Never log the full topic/token.
- Redact in exports unless the user explicitly chooses otherwise.
- Keep ntfy disabled by default.

---

## Generic ntfy request shape

```http
POST /<topic> HTTP/1.1
Host: ntfy.sh
Title: wscan+ alert
Priority: high
Tags: warning,wscanplus

Alert body text
```

JavaScript example:

```js
await fetch(`${serverUrl}/${topic}`, {
  method: 'POST',
  headers: {
    Title: 'wscan+ alert',
    Priority: 'high',
    Tags: 'warning,wscanplus',
  },
  body: 'Alert body text',
});
```

---

## Priority mapping

Suggested ntfy priority mapping:

| wscan+ severity | ntfy priority | Tags |
| --- | --- | --- |
| Critical / High | `urgent` or `5` | `rotating_light,warning,wscanplus` |
| Medium / Watch | `high` or `4` | `warning,wscanplus` |
| Low | `default` or `3` | `information_source,wscanplus` |
| Info | local only by default | `wscanplus` |

Default policy: send only High/Critical externally.

---

## Usage examples

### 1. Desktop threat queue alert

When the detector raises a high-confidence threat:

```text
Title: wscan+ threat detected
Priority: urgent
Tags: rotating_light,warning,wscanplus,wifi
Body: High-confidence rogue AP pattern detected near trusted SSID. BSSID F2:1D:02:90:11:FE. Confidence 82%.
```

Use for:

- rogue AP / evil-twin-like pattern
- security downgrade
- suspicious BSSID collision
- deauth storm indicator
- Flipper/Marauder signature match

### 2. Orbic RC400L / Rayhunter cellular alert

Rayhunter can expose analysis through the Orbic RC400L on port 8080 after ADB forwarding:

```bash
adb forward tcp:8080 tcp:8080
```

wscan+ can poll:

```text
GET http://localhost:8080/api/qmdl-manifest
GET http://localhost:8080/api/analysis-report/{name}
GET http://localhost:8080/api/system-stats
```

High severity Rayhunter event:

```text
Title: wscan+ cellular warning
Priority: urgent
Tags: rotating_light,warning,cellular,rayhunter,wscanplus
Body: Rayhunter reported High severity cellular anomaly: Null cipher usage detected on NAS layer.
```

Recommended behavior:

- send High by default if ntfy is enabled
- send Medium only if user configured Medium threshold
- keep Low/Informational local by default
- dedup by recording name + packet timestamp + message

### 3. Companion sensor offline alert

If an Android companion was paired and then stops sending heartbeat updates:

```text
Title: wscan+ companion offline
Priority: high
Tags: warning,android,wscanplus
Body: Android companion Pixel sensor heartbeat stale for 120 seconds. Last sequence: 418.
```

Use for:

- companion heartbeat stale
- sequence rollback or invalid sequence
- payload rejected repeatedly
- unauthorized/offline ADB device

### 4. Scan/session milestone notification

For long-running scans:

```text
Title: wscan+ scan milestone
Priority: default
Tags: satellite,wscanplus
Body: Field scan session completed. 64 APs observed, 3 watched, 1 high-risk event.
```

Use for:

- scan started
- scan stopped
- session completed
- export created
- baseline captured

These should be lower priority and opt-in separately from threat alerts.

### 5. Baseline drift alert

When a trusted environment changes materially:

```text
Title: wscan+ baseline drift
Priority: high
Tags: warning,chart_with_upwards_trend,wscanplus
Body: Baseline changed: 2 trusted APs missing, 1 known SSID changed security mode, 4 new APs observed.
```

Use for:

- known AP missing
- trusted SSID changed security mode
- known BSSID moved channel unexpectedly
- unexpected RSSI drift beyond threshold

### 6. Export/report ready notification

When a local report is ready:

```text
Title: wscan+ report ready
Priority: default
Tags: clipboard,wscanplus
Body: Session report exported locally. 12 findings, 4 watched networks, 1 cellular event.
```

Do not attach sensitive reports by default. Prefer a local UI notification unless the user explicitly enables external notification.

### 7. System health alert

Desktop or sensor health events:

```text
Title: wscan+ sensor health
Priority: high
Tags: warning,battery,wscanplus
Body: Orbic RC400L battery low or Rayhunter system stats unavailable.
```

Use for:

- Orbic/Rayhunter API unreachable
- low battery
- low disk
- scanner loop failure
- repeated command timeouts
- companion server bind failure

### 8. CI / project operations alert

For developer workflow, ntfy can notify when CI fails or a release build is ready.

```text
Title: wscan+ CI failed
Priority: high
Tags: warning,computer,wscanplus
Body: Agent Guarded CI failed on Android Unit Tests for PR #123.
```

Keep this separate from runtime user alerts.

### 9. Operator panic / manual alert

Manual operator action from desktop UI:

```text
Title: wscan+ operator note
Priority: urgent
Tags: rotating_light,wscanplus
Body: Operator manually flagged current environment for review.
```

This should be explicit user action only.

### 10. Lab simulation notification

For controlled lab tests:

```text
Title: wscan+ lab event
Priority: default
Tags: test_tube,wscanplus
Body: GL.iNet rogue AP simulation started. Watch for evil-twin detector output.
```

Useful for correlating simulation start/stop times with detector telemetry.

---

## Event mapper sketch

```js
function mapEventToNtfy(event) {
  if (event.type === 'rayhunter' && event.severity === 'High') {
    return {
      title: 'wscan+ cellular warning',
      priority: 'urgent',
      tags: 'rotating_light,warning,cellular,rayhunter,wscanplus',
      body: event.message,
    };
  }

  if (event.type === 'wifi-risk' && event.severity === 'high') {
    return {
      title: 'wscan+ threat detected',
      priority: 'urgent',
      tags: 'rotating_light,warning,wifi,wscanplus',
      body: event.summary,
    };
  }

  return null;
}
```

---

## Dedup and rate limiting

Required controls:

```text
dedupKey = source + severity + stable finding id
cooldown = 60s to 300s depending on severity
maxBurst = small number per window
```

Examples:

- Do not notify every repeated scan result.
- Do not notify every unchanged Rayhunter report poll.
- Notify when severity increases.
- Notify when a new unique threat appears.
- Notify when a stale companion changes from warning to recovered.

---

## Privacy and security notes

Do not send by default:

- precise GPS coordinates
- full SSID lists
- full BSSID inventories
- raw Rayhunter report JSON
- pairing tokens
- ntfy topic URL
- user names or home address labels

Safe default body style:

```text
wscan+ detected a high severity wireless/cellular event. Open the desktop hub for details.
```

Allow richer bodies only by explicit setting.

---

## Implementation phases

### Phase 1: settings and test notification

- add ntfy settings model
- add test notification button
- validate URL/topic
- redact topic/token in logs

### Phase 2: desktop event publisher

- map high-risk desktop detector events
- local event-console logging
- dedup and rate limit

### Phase 3: Orbic/Rayhunter integration

- Rayhunter poller normalizes reports into cellular risk events
- ntfy sends high severity cellular alerts if enabled

### Phase 4: companion/system health

- companion stale heartbeat
- ADB unauthorized/offline transitions
- Rayhunter API unreachable
- export/report complete

---

## Non-goals

- ntfy is not required for core detection.
- ntfy failures must never block scanning, detection, export, or companion ingest.
- No Google Maps work.
- No hard-coded public topic.
- No automatic cloud alerting until the user enables it.
