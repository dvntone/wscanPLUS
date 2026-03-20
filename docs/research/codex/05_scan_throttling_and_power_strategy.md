# Scan Throttling and Power Strategy (Android)

Created: 2026-03-19
Status: Findings grounded in Android WiFi scan limitations

## Reality Check

Android limits active WiFi scans to protect battery and network performance.
Foreground apps are limited to 4 scans per 2 minutes.
Background apps are limited to 1 scan per 30 minutes (shared across apps).

Reference: Android WiFi scanning docs
https://developer.android.com/develop/connectivity/wifi/wifi-scan

## Strategy to Preserve Battery and Effectiveness

1. Passive listening first
- Use scan results broadcasts and callbacks without calling startScan when possible.
- This leverages system scans and reduces radio usage.

2. Adaptive scan cadence
- Trigger startScan only when movement or network state changes.
- Use burst scans only when the user requests verification.

3. User-visible modes
- Passive mode for baseline monitoring
- Verify mode for short high-cadence runs
- Stationary mode for plugged-in or tethered use

4. Delta-based processing
- Run heavy heuristics only when results materially change.

## Outcome

This approach reduces battery drain while preserving core detection goals.
