# Desktop Hub Requirements (Linux Debian, Kali Test Env)

Status: Proposed alignment checklist for desktop hub work

## Platform and Environment

- Target OS: Debian-based Linux
- Test environment: Kali Linux
- Desktop app is the primary hub for aggregation, analysis, and reporting

## Required Capabilities

1. Device connectivity
- Connect to Android via ADB port forwarding
- Support multiple devices by serial

2. Data ingestion
- Accept NDJSON messages over a local socket
- Store raw scan results and threat signals

3. Correlation and baseline
- Multi-device timeline aggregation
- Baseline network density and anomaly scoring

4. Evidence and reporting
- Export PCAP or PCAPNG when available
- Export JSON sessions
- Generate a human-readable incident brief

5. Security and process
- Enforce local-only CORS policy
- Apply required HTTP headers for local UI
- Never run as root

## Deliverables for Phase 5

- Desktop data model for multi-device storage
- Correlation engine stub with clear interfaces
- Initial UI for timeline and baseline comparison

## Notes

- Aligns with docs/SESSION_STATE.md and docs/SECURITY.md
- This file is a guide to keep desktop work focused and testable
