# Integration Gaps (Android Companion <-> Linux Desktop Hub)

Status: Identified gaps based on current repo docs

## Gaps

1. Message schema is not defined
- There is no concrete payload format for scan results, threat signals, or GPS updates.
- Action: define message schemas per type in the data exchange contract.

2. Handshake and versioning are not defined
- No protocol version negotiation or device identity handshake is specified.
- Action: add device_identity and protocol version requirements.

3. Retry and idempotency rules are not defined
- No rules for duplicate delivery or ack timeouts are specified.
- Action: standardize correlationId and ack semantics.

4. Backlog sync behavior is not defined
- Store and sync mode is named, but there is no explicit backlog query or batching.
- Action: define backlog_batch and request_backlog messages.

5. Desktop correlation layer is not specified
- Multi-device is required, but correlation across devices and time windows is not defined.
- Action: add a desktop-side correlation spec and a baseline model.

6. Evidence bundle format is not defined
- Export formats are named, but no incident package or integrity proof is specified.
- Action: define an incident bundle with hashes and a readable summary.

7. Kali lab validation plan is not defined
- Hardware is listed, but there is no repeatable test matrix for detections.
- Action: define a Kali-based validation protocol with expected outputs.

## Notes

- These are doc-level gaps only and do not require code changes.
- Addressing them will reduce ambiguity for both Android and desktop implementation.
