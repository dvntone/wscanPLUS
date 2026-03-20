# Data Exchange Contract (Android <-> Linux Desktop Hub)

Status: Proposed addition based on current docs
Source alignment: docs/SESSION_STATE.md (ADB boundary + sync modes)

## Scope

Defines the message contract and transport rules between Android companion app and Linux desktop hub.
Transport is ADB port forwarding to Android localhost:9000 as documented.

## Transport

- Android listens on localhost TCP port 9000.
- Desktop connects via ADB port forwarding to the device.
- Connection priority: USB ADB, then wireless ADB, then remote sync.

## Framing

- Line-delimited JSON (NDJSON), UTF-8.
- Each line is one complete message object.
- If payloads grow large (PCAP or JSON exports), use chunked transfer with explicit chunk ids.

## Envelope (All Messages)

Fields
- version: string (protocol version)
- type: string (message type)
- deviceId: string (ADB serial)
- sentAt: number (epoch ms)
- correlationId: string (uuid)
- payload: object (type-specific)

## Message Types

From Android to Desktop
- scan_results
- threat_signals
- gps_update
- tether_status
- device_heartbeat
- device_identity
- backlog_batch
- export_ready

From Desktop to Android
- ack
- request_backlog
- set_config
- request_export
- alert
- sync_mode

## Acknowledgment

- Every message expects an ack with matching correlationId.
- Android retries if no ack within timeout.
- Desktop must be idempotent on retries.

## Backlog Sync

- Android stores offline results locally.
- Desktop sends request_backlog with time range and max batch size.
- Android responds with backlog_batch messages until complete.

## Security Assumptions

- ADB channel is local and authenticated by user.
- Do not assume trust beyond the ADB tunnel.
- Include protocol versioning for forward compatibility.
