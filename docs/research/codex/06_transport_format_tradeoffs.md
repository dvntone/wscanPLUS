# Transport Format Tradeoffs (NDJSON vs Protobuf)

Created: 2026-03-19
Status: Guidance for Android <-> Desktop message format choice

## NDJSON

Pros
- Human readable
- Simple to implement
- Easy to debug and log

Cons
- Larger payload size
- Loose schema validation

## Protobuf

Pros
- Compact payloads
- Strong schema and versioning
- Faster parsing at scale

Cons
- Requires schema management and code generation
- Adds tooling overhead to Android and Electron stacks

## Practical Recommendation

- Start with NDJSON for Phase 3 and early Phase 5 integration
- Move to Protobuf after message types stabilize and bandwidth becomes a concern

## Notes

- Protobuf can be introduced behind the same ADB transport without changing the socket model
- Versioned envelopes are required for either format
