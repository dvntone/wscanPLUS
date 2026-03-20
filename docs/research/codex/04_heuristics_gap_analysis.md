# Heuristics Gap Analysis (WIDS Reference vs Phase 2)

Created: 2026-03-19
Status: Findings grounded in WIDS references and current Phase 2 heuristics

## Phase 2 Heuristics (Current)

- WEP/Open detection
- Evil twin
- Encryption downgrade
- Karma attack
- SSID flooding
- RSSI anomaly
- BSSID fingerprinting

## Gaps vs WIDS Reference (Kismet Alerts)

- Advertised encryption change alerts are not explicitly covered
- Beacon rate change alerts are not explicitly covered
- Probe response channel mismatch alerts are not explicitly covered
- Allowlist-based AP spoofing alerts are not explicitly covered
- Trend-based flooding or DoS alerts are not explicitly covered

Reference: Kismet alerts documentation
https://www.kismetwireless.net/docs/readme/alerts/alerts/

## Why These Gaps Matter

- Encryption change and beacon rate shifts indicate rogue AP or spoofing
- Probe channel mismatch can flag misdirection or fake AP behavior
- Allowlist spoofing is one of the strongest rogue AP detectors for known SSIDs
- Trend alerts catch flood or DoS patterns that single-scan heuristics may miss

## Suggested Placement

- These belong in Phase 2 or Phase 3 as additions to the local heuristic layer
- If added later, they can be implemented as pure Kotlin checks using existing scan history
