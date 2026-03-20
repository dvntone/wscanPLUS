# Privacy Boundaries (Non?Play?Store Context)

**Date**: 2026-03-19 (America/Los_Angeles)

## Purpose
Identify sensitive fields that should be protected or redacted by default, even outside Play Store constraints.

## High?Sensitivity Fields
- Exact GPS coordinates (lat/long)
- Precise timestamps tied to location
- BSSID/MAC addresses
- Device identifiers and SSIDs
- Bluetooth device names / addresses

## Default Handling (Normal Operation)
- Store raw identifiers only when user opts?in.
- Redact or hash identifiers for default logs.
- Separate local debug logs from exportable reports.
- Clearly label mock?location data and prevent it from influencing real alerts.

## Rogue?Agent Exception (Important)
- If a rogue agent is detected, **identifiers may be captured in full** for evidence and response.
- This overrides the default redaction policy and should be recorded as an explicit event state (e.g., `evidence_mode=true`).

## Notes
These boundaries are **policy**, not OS?enforced. They reduce risk and increase user trust even in non?Play?Store builds.
