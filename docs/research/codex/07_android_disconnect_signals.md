# Android Disconnect and Error Signals (Input Catalog)

Created: 2026-03-19
Status: Documented signals only, with sources
Purpose: Provide inputs that can feed proactive and reactive detection flows

## Key Signal Sources (Documented)

1. SupplicantState
- Enum includes AUTHENTICATING, ASSOCIATING, FOUR_WAY_HANDSHAKE, GROUP_HANDSHAKE, COMPLETED, DISCONNECTED, DORMANT, SCANNING, INTERFACE_DISABLED, and others.
- Source: https://developer.android.com/reference/android/net/wifi/SupplicantState

2. NetworkInfo.DetailedState
- Includes AUTHENTICATING, CONNECTING, OBTAINING_IPADDR, DISCONNECTED, FAILED, VERIFYING_POOR_LINK, CAPTIVE_PORTAL_CHECK, BLOCKED, and more.
- Source: https://developer.android.com/reference/kotlin/android/net/NetworkInfo.DetailedState

3. WifiManager SUPPLICANT_STATE_CHANGED_ACTION and extras
- Broadcast includes EXTRA_NEW_STATE and EXTRA_SUPPLICANT_ERROR.
- These are deprecated as of API 28 but still documented.
- Source: https://developer.android.com/reference/android/net/wifi/WifiManager

4. ERROR_AUTHENTICATING constant
- Defined in AOSP WifiManager as a supplicant error code (deprecated but present).
- Source: https://android.googlesource.com/platform/packages/modules/Wifi/+/4042db41d23e62899d777f381e5663360b9fdc96/framework/java/android/net/wifi/WifiManager.java

## Notes on Deprecation

- Many supplicant broadcasts and extras are deprecated in API 28, but still exist in the framework API.
- Any implementation must account for API level differences and use fallbacks where needed.

## Proposed Proactive vs Reactive Flow (Policy, Not Final)

This section is a policy scaffold only. Thresholds and time windows are intentionally not specified and must be tuned with real data.

Proactive phase inputs
- Repeated transitions across AUTHENTICATING, ASSOCIATING, FOUR_WAY_HANDSHAKE without reaching COMPLETED
- Repeated DISCONNECTED or FAILED states within short observation windows
- Frequent AUTHENTICATING followed by ERROR_AUTHENTICATING when available

Reactive phase trigger
- Escalate to reactive mode when proactive signals exceed the tuned threshold or persist across multiple observation windows
- Reactive mode can enable higher scan cadence and increase evidence capture frequency

## Constraints

- These signals are indicators only, not proof of an attack.
- Interpretation requires context and should be combined with existing local heuristics.
