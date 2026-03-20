# Device Testing

This area separates repeatable test procedures from device-specific evidence.

## Layout

- `shared/` - reusable checklists and operator guidance that apply across devices
- `devices/motorola-g4-play-2024/` - archived findings from the previous Android test device
- `devices/revvl-tab-2/` - active prep and session notes for the current Android 15 tablet target

## Rules

- Keep durable summaries and conclusions in markdown under the relevant device folder.
- Keep raw captures such as logcat dumps local-only under `artifacts/`; those paths are git-ignored.
- If a test is repeated on a new device, create or update that device's README instead of mixing results into another device archive.
