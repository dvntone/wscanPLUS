# Threat Context (Real-World Motivation)

## Summary

wscan+ is motivated by repeated, suspicious wireless and network events experienced in a multi-unit apartment environment. The observed pattern is consistent with a **proximate, human-operated attacker** — likely within the same building or an adjacent complex — rather than a purely remote or internet-based threat. The most harmful gap was not only detection, but **being able to explain and document** what was happening to non-technical people and third parties.

This document captures the threat model and the product requirements it implies. It is intended to keep collaborators aligned on *why* certain features are prioritised.

---

## Environment

- Multi-unit apartment complex with high device density (veterans housing; many residents are older, disabled, or not technically experienced).
- Observed wireless anomalies significantly exceed what is typical for a residential building.
- Primary connectivity shifted away from home internet to reduce exposure (see "Current Defensive Posture").

---

## Key Observations

- **Network flooding / extreme SSID counts:** Scanning tools intermittently show hundreds to ~1,000 visible networks — inconsistent with typical residential density and suggestive of beacon spam or aggressive nearby scanning.
- **Timing correlation:** Suspicious activity often increased when arriving home or during predictable routines, implying an attacker reacting to presence or schedule.
- **Reactive behaviour:** Activity sometimes ceased temporarily when physically checking outside, reinforcing the hypothesis of a manual operator in close proximity.
- **Device anomalies:** Phones becoming unusually hot during incidents, consistent with sustained wireless scanning, forced reconnect loops, or heavy radio/CPU activity.
- **Escalation pattern:** Defensive actions (learning tools, SSID rotation, device lockdown) correlated with increased interference, consistent with an adversary monitoring and adapting.

---

## Impact

- Loss of trust in home network and connected devices.
- **Credibility gap:** difficult to communicate the situation to non-technical people (neighbours, building management, support services) without tangible evidence.
- Concern that a low-technical-literacy population may be broadly targeted (credentials, financial accounts, privacy monitoring), not just a single individual.
- **Privacy beyond credentials:** peer-reviewed research demonstrates that a rogue AP is not only a credential-theft risk but a through-wall surveillance platform capable of tracking presence, movement, and activity (see WiFi-Based Passive Surveillance section).

---

## Current Defensive Posture

- Home internet cancelled to reduce the attack surface.
- Reduced use of IoT devices (streaming devices, voice assistants, Wi-Fi cameras, consoles).
- Reliance on multiple carrier-unlocked cellular devices and multiple SIMs to allow rapid rotation if a device or SIM is compromised.

---

## Threat Model (Working Assumptions)

- Attacker is likely **local and proximate** (same building or close vicinity), capable of:
  - Rogue AP / evil-twin attempts
  - Deauthentication and disassociation pressure
  - Beacon / SSID flooding to create noise and obscure activity
  - Opportunistic credential theft via MITM or social engineering
  - **Passive presence and movement monitoring via WiFi CSI** (see section below)
- Attacker may be persistent and may target populations with limited technical literacy.
- Remote/internet threats are considered secondary; local RF-layer attacks are the primary concern.

---

## WiFi-Based Passive Surveillance (Peer-Reviewed Research)

Standard consumer WiFi hardware can be repurposed as a silent, through-wall surveillance platform. This is not theoretical — it is demonstrated in peer-reviewed academic research using inexpensive commodity equipment.

### What the Research Shows

**Carnegie Mellon University — "DensePose From WiFi" (2023)**
Using two standard 802.11 routers (~$60 total), CMU researchers generated full-body 3D pose maps of people in the same room — posture, limb positions, activity patterns, and movement tracking — using only standard WiFi Channel State Information (CSI). No cameras. No microphones. No specialized hardware. Performance approaches image-based pose estimation in the same environment.

**La Sapienza University, Rome — "WhoFi" (2025)**
Using the same class of hardware, researchers achieved **95.5% accuracy** in re-identifying specific individuals by their unique "radio biometric signature" — the characteristic way a person's body alters WiFi signals as they move through space. The system distinguishes person A from person B through walls, across clothing changes, without any camera data.

### What This Means for This Threat Model

A rogue AP or evil-twin device deployed in a hallway, adjacent apartment, or shared space can — simultaneously — perform credential theft AND silently:

1. **Detect presence** — is the target currently home?
2. **Track movement** — where in the unit are they moving?
3. **Estimate activity** — sleeping, sitting, in the bathroom, walking to the door
4. **Re-identify specific individuals** — distinguish resident A from resident B
5. **Build a behavioral profile** — routine, schedule, occupancy patterns across days and weeks

No cameras. No audio. No visible intrusion. No victim awareness. The required hardware costs approximately $60.

This capability does not require the attacker to compromise any device or intercept any traffic. The signals are ambient — every WiFi-capable device in the area passively emits the RF that enables this.

### Implication for Evidence Framing

When wscan+ detects and documents a rogue AP, the significance of that detection is materially different than "someone may be trying to capture your Wi-Fi password."

**Upgraded threat statement for non-technical communication:**
> "The device detected near your unit can silently track movements through walls without cameras, confirm when you are home, and distinguish between different people in the space — peer-reviewed research from Carnegie Mellon University demonstrates this using hardware available for $60."

This framing is factually accurate, cites verifiable academic sources, and communicates the actual risk to building management, law enforcement, or support services in terms they will take seriously.

### Detection Signals (New)

An AP performing CSI-based sensing typically exhibits a characteristic configuration:
- **Fixed channel** — no roaming (CSI sensing requires a stable channel to build spatial models)
- **Elevated beacon rate** — more frequent beacons = more CSI samples = higher sensing resolution
- **Wide channel width** (40/80 MHz) — more subcarriers = more spatial information encoded per sample
- **Minimal associated clients** — the AP is not being used for actual internet access
- **Near-zero data traffic ratio** — high beacon activity, negligible data throughput

These signals are available in current `WifiScanResult` fields and represent a candidate heuristic for future development.

### References
- Geng, Huang, De la Torre. "DensePose From WiFi." *arXiv:2301.00250*. Carnegie Mellon University, 2023.
- Avola, Emam, Montagnini, Pannone, Ranaldi. "WhoFi: Deep Person Re-Identification via Wi-Fi Channel Signal Encoding." *arXiv:2507.12869*. La Sapienza University of Rome, 2025.

---

## Product Requirements Implied

### Must-have (Baseline — non-technical users)

- Passive-friendly detection with plain-language alerts (avoid jargon).
- "Proof" artefacts: human-readable summaries with timestamps that a non-technical person can share or present.
- Export / share capability (screenshots, PDF, basic reports).

### Advanced / Pro (security professionals)

- Evidence-grade exports (e.g. `.pcap`, JSON) compatible with analysis tools such as Wireshark.
- Event timelines, anomaly baselining, and correlation (e.g. "network count spike" detection).
- Optional AI-assisted summarisation that converts raw events into coherent, human-readable incident narratives.

---

## Device-Level Threat Vectors (Observed / Suspected)

These are local device behaviours that correlate with the wireless threat pattern and represent candidate detection signals for future phases.

- **Camera / microphone access on untrusted SSIDs:** Apps with legitimate camera/mic permissions can silently capture and transmit audio/video data while connected to a rogue AP or evil-twin. Android governs camera and microphone use through app permissions and sensor APIs — the real risk is outbound exfiltration by apps that already have those permissions, not inbound access. Detection signal: apps opening camera/mic handles while the device is connected to an anomalous SSID.
- **Sustained CPU/radio activity:** Phones becoming unusually hot during network anomaly windows is consistent with forced reconnect loops, sustained wireless scanning, or active background data exfiltration.
- **ADB-exposed devices:** Devices with USB debugging enabled and connected to a shared or untrusted network may be reachable via ADB over TCP (port 5555). Detection signal: unexpected inbound connection attempts to ADB port on-device.

> These vectors are documented for detection scope planning only. wscan+ does not exploit or actively probe them.

---

## Non-goals / Safety

- wscan+ must not encourage illegal activity or active wireless interference.
- Data collection should be minimal and user-controlled; all exports require explicit user action.
- User safety and privacy take priority over feature completeness.
- Attribution is not a goal — wscan+ detects and documents anomalies, it does not identify attackers.
- This document does not contain private identifying details (no names, addresses, or specific unit information).
