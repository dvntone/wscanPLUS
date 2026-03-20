# WiGLE .m8b Export (ADB + Spec)

**Date**: 2026-03-19 (America/Los_Angeles)

## Source File (device)
- `/sdcard/Documents/export.m8b`
- Size: 1191 bytes
- Modified: 2026-03-19 08:40

## Quick Check (device)
- File is **binary**, not plain text.
- Header begins with:
  - `MJG`
  - `2`
  - `SIP-2-4`
  - `1e`
  - `MGRS-1000`

## Spec Summary (WiGLE m8b)
From the official m8b repository README:
- M8B is a compact, offline artifact to estimate location from observed identifiers (e.g., Wi?Fi MACs) without storing the full dataset.
- Identifiers are hashed with SipHash?2?4 (fixed key), truncated to **n** bits to intentionally allow collisions.
- Coordinates are encoded into MGRS 1?km squares (9?byte string format).
- The file is **header + body**:
  - Header is 8 newline?terminated UTF?8 lines:
    - `MJG` (magic)
    - version (`2`)
    - hash (`SIP-2-4`)
    - slicebits (hex)
    - coords (`MGRS-1000`)
    - idsize (hex bytes)
    - coordsize (hex bytes)
    - record count (hex)
  - Body is concatenated fixed?size records: `<identifier><coordinate>`, sorted by identifier, enabling binary search.

## Implication for WSCAN+
- CSV/KML exports are best for **schema/reference**.
- `.m8b` is useful for **offline coarse location** from identifier sets, but requires implementing the header + fixed?record parsing described above.

## Privacy
No data bytes were recorded in this document due to sensitive identifiers and location coordinates.
