# Motorola G4 Play 2024 Archive

This folder archives the prior physical-device verification work completed before the Revvl Tab 2 session started.

## Device

- Model: `moto g play - 2024`
- ADB serial seen during prep: `ZY22KFCNSK`
- Role: previous baseline Android phone used for ADB, permissions, scan-behavior, Bluetooth, and export validation

## Contents

- `findings/` contains the durable markdown summaries from the Motorola session.
- `artifacts/` contains raw local captures that remain git-ignored.

## Notes

- Treat the findings here as historical evidence, not proof that OEM-specific behavior will match on the Revvl Tab 2.
- Re-run permission, scan behavior, and service lifecycle checks on the new device before assuming parity.
