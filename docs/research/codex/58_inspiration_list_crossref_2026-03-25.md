# Inspiration List Cross-Reference

**Date:** 2026-03-25
**Source:** dvntone GitHub "Inspiration" starred list (11 repos)
**Purpose:** Map each repo to wscanplus features, gaps, and planned work

---

## Repos in the List

1. Hack-with-Github/Awesome-Hacking
2. 0n1cOn3/FluxER
3. raymondSeger/lscript
4. feross/spoof
5. GyulyVGC/awesome-pentest
6. GyulyVGC/awesome-pcaptools
7. GyulyVGC/sniffnet
8. derv82/wifite2
9. P0cL4bs/wifipumpkin3
10. larbi1512/WLAN-Intrusion-Detection-with-AI-
11. hackcrypto/fluxion

---

## Detailed Cross-Reference

### P0cL4bs/wifipumpkin3

**What it is:** Python rogue AP framework. Deploys a device that broadcasts multiple SSIDs simultaneously, responds to any probe request with a matching SSID (Karma attack), and runs a captive portal to harvest credentials.

**Mapped to wscanplus:**
- `KarmaHeuristic.kt` — directly models this attack. Single BSSID broadcasting 3+ distinct SSIDs. Confidence tiers: 3+ = 0.45, 5+ = 0.75, 10+ = 0.95. Same-channel + tight RSSI cluster adds 0.1 bonus (hardware signature of a Pineapple/wifipumpkin3 device).
- `EvilTwinHeuristic.kt` — covers the rogue AP side: OUI mismatch, security capability mismatch, new BSSID for known SSID, RSSI anomaly.

**Gap revealed:** wifipumpkin3 also does DHCP/DNS spoofing and captive portal injection. wscanplus catches the RF signature but not the network-layer MITM payload. VpnService pipeline is deferred — this gap is intentional and documented.

---

### hackcrypto/fluxion + 0n1cOn3/FluxER

**What they are:** Shell-based evil twin attack tool. Attack sequence: (1) deauth clients from the real AP, (2) clone the AP as Open, (3) serve a fake captive portal, (4) capture WPA handshake. FluxER wraps Fluxion for Termux on Android.

**Mapped to wscanplus:**
- `EvilTwinHeuristic.kt` — catches the clone AP. Same SSID, different BSSID, security capability mismatch (Fluxion clones as Open to serve the portal).
- `EncryptionDowngradeHeuristic.kt` — catches the WPA2→Open downgrade step.

**Gap revealed:** The deauth flood (step 1) is not detectable by wscanplus. This requires `wpa_supplicant` reason code monitoring via `READ_LOGS` ADB permission — explicitly deferred in ROADMAP (ADB-elevated detection, F-Droid/sideload only path). The captive portal itself requires network-layer inspection (VpnService, also deferred). Deauth detection is the single biggest blind spot against this attack class.

---

### feross/spoof

**What it is:** CLI tool for randomizing MAC addresses on macOS, Windows, and Linux.

**Mapped to wscanplus:**
- `BssidFingerprintHeuristic.kt` — tracks BSSID fingerprints over time and flags rotation.
- OUI mismatch sub-signal in `EvilTwinHeuristic.kt` catches vendor spoofing within a single scan.

**Gap revealed:** Cross-session BSSID rotation (attacker periodically rotating their AP's MAC across days) would accumulate multiple fingerprint records rather than triggering the rotation heuristic in a single session. This requires the scan history accumulation work (Phase 2 follow-on: `knownProfiles` population) to be completed before it is detectable.

---

### derv82/wifite2

**What it is:** Automated WiFi auditing tool. Attacks: WPS brute force, WPA/WPA2 handshake capture via deauth, PMKID attack.

**Mapped to wscanplus:**
- Passive scanning phase: wscanplus does the same from the defender side.
- Deauth-to-capture workflow: exposes the same deauth detection gap as Fluxion.

**Gap revealed:** PMKID attack is completely undetectable by the current heuristic set. PMKID is extracted from the 4-way handshake beacon without requiring client deauth — it leaves no RF anomaly (no SSID flooding, no RSSI anomaly, no BSSID rotation). This is a known gap, low priority to address since it leaves no observable signal at the scan layer.

---

### larbi1512/WLAN-Intrusion-Detection-with-AI-

**What it is:** Jupyter Notebook project using ML models (Random Forest, SVM, etc.) trained on WLAN traffic features to classify attacks — deauth floods, probe floods, evil twin, etc.

**Mapped to wscanplus:**
- The deferred "On-Device ML (LiteRT)" tier in ROADMAP is the direct counterpart.
- Feature set used for training (SSID count, BSSID, RSSI, security type, channel) overlaps with what wscanplus already collects.
- `SsidFloodingHeuristic.kt` uses a manual z-score baseline that approximates what this repo does statistically with trained models.

**Useful now:** The feature engineering methodology — which metrics matter for classification, what thresholds distinguish attack from noise — applies directly to improving current heuristic thresholds and informing the baseline population design. Also useful for designing the LiteRT training dataset when that tier is reached.

**Difference:** This repo works on PCAP/traffic data. wscanplus works on scan results only (no raw frames without monitor mode on Android).

---

### GyulyVGC/sniffnet

**What it is:** Real-time network traffic monitor in Rust. Captures packets, shows per-connection stats, filters by protocol/host, cross-platform. Uses pcap.

**Mapped to wscanplus:**
- Desktop hub Phase 5 — the "unified timeline + map overlay" and per-connection view align with sniffnet's output.
- THREAT_CONTEXT.md calls for "evidence-grade exports compatible with Wireshark" and connection-level visibility during incidents. Sniffnet is a working example of what this looks like.

**Note:** sniffnet is a standalone tool, not a library. wscanplus desktop hub would implement similar capability independently, likely via a pcap binding.

---

### GyulyVGC/awesome-pcaptools

**What it is:** Curated list of tools for processing PCAP/pcapng network trace files — parsing libs, analysis tools, visualization.

**Mapped to wscanplus:**
- Relevant to the Kismet/BetterCap integration planned for the desktop hub (Phase 5).
- THREAT_CONTEXT.md "Advanced/Pro" requirements include `.pcap` exports compatible with Wireshark.
- No PCAP capability exists in wscanplus currently — all data is scan results, not raw frames.

---

### Hack-with-Github/Awesome-Hacking

General curated list of hacking resources for pentesters and security researchers. No specific code mapping to wscanplus. General research reference.

---

### GyulyVGC/awesome-pentest

Curated list of penetration testing resources and tools. No specific code mapping. General research reference.

---

### raymondSeger/lscript

Collection of scripts for automating common pentesting tasks. No direct mapping to wscanplus functionality.

---

## Ranked by Current Relevance

| Rank | Repo | Why |
|------|------|-----|
| 1 | P0cL4bs/wifipumpkin3 | Karma + Evil Twin heuristics were built to detect this exact tool. Validate thresholds against its default behavior. |
| 2 | hackcrypto/fluxion | Exposes the deauth detection gap — the biggest current blind spot. Step 1 of Fluxion's attack is invisible without ADB reason code monitoring. |
| 3 | larbi1512/WLAN-Intrusion-Detection-with-AI- | Feature engineering methodology applies directly to heuristic threshold tuning now, and LiteRT model design later. |
| 4 | feross/spoof | Points at the scan history accumulation gap. Cross-session BSSID rotation undetectable until `knownProfiles` population is complete. |
| 5 | derv82/wifite2 | Reveals PMKID as an undetected attack vector. No urgency — leaves no RF anomaly at the scan layer. |
| 6 | GyulyVGC/sniffnet | UX reference for the desktop hub connection-level evidence view. Phase 5. |
| 7 | GyulyVGC/awesome-pcaptools | Reference for desktop hub PCAP integration. Phase 5. |
| 8 | Hack-with-Github/Awesome-Hacking | General research reference. |
| 9 | GyulyVGC/awesome-pentest | General research reference. |
| 10 | raymondSeger/lscript | Not currently relevant. |

---

## Key Gaps This Analysis Surfaces

1. **Deauth detection** — the most impactful gap. Required by Fluxion, wifite2, and any deauth-based attack. Needs `READ_LOGS` ADB permission (deferred, F-Droid/sideload path).
2. **Scan history accumulation** — `knownProfiles` and baseline population not yet built (Phase 2 follow-on). Blocks cross-session BSSID rotation detection and improves SSID flooding z-score accuracy.
3. **PMKID** — undetectable at the scan layer. Documented known gap, low priority.
4. **Network-layer MITM** — captive portal / DHCP spoof detection requires VpnService (deferred, out of scope for companion app).
