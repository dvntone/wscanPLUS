# Device Testing

This area separates repeatable test procedures from device-specific evidence.

## Layout

- `shared/` - reusable checklists and operator guidance that apply across devices
- `shared/49_background_keyguard_scan_matrix.md` - repeatable unlocked / HOME / secure-keyguard / post-unlock matrix for scan continuity
- `devices/motorola-g4-play-2024/` - archived findings from the previous Android test device
- `devices/revvl-tab-2/` - active prep and session notes for the current Android 15 tablet target

## Rules

- Keep durable summaries and conclusions in markdown under the relevant device folder.
- Keep raw captures such as logcat dumps local-only under `artifacts/`; those paths are git-ignored.
- Hard rule: do not place device serials or other persistent device identifiers in tracked docs, issues, PR text, or commit messages.
- If a test is repeated on a new device, create or update that device's README instead of mixing results into another device archive.
- Cross-device behavior changes that affect the product model should also be summarized once under `shared/`.
