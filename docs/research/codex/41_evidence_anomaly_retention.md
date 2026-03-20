# Evidence & Anomaly Data Retention

**Date**: 2026-03-19 (America/Los_Angeles)

## Purpose
Capture the project stance that anomaly detection requires retaining identifiable data (timeframes, RSSI, MACs/SSIDs/BSSIDs, approximate geo) for meaningful analysis.

## Policy (Project Requirement)
- **Anomaly detection requires data retention.**
- The system should retain:
  - Timestamps / time windows
  - RSSI / signal strength history
  - SSID / BSSID / MAC identifiers
  - Approximate geolocation (cell?tower distance / coarse GPS)

## Why This Matters
- Without retention, it is impossible to detect patterns over time (e.g., weekend?only attacks, recurring rogue agents, or location?specific anomalies).
- Evidence?grade records support user trust and remediation.

## Operational Implications
- Storage must support time?series queries.
- Data should be indexed by time, network identifier, and location.
- Retention should be configurable but **enabled by default** for anomaly detection.

## Relationship to Privacy Boundaries
- This policy **overrides default redaction** when anomaly or rogue?agent conditions are present.
- Normal operation can still use redaction where appropriate.
