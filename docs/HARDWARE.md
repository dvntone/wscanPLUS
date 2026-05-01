# wscanplus Hardware Reference

This document describes the hardware available for development and testing of wscanplus. All attack scenarios are simulated in a controlled, private lab environment for the sole purpose of building and validating defensive detection capabilities.

---

## 📡 Wireless Adapters

| Adapter | Chipset | Monitor Mode | Packet Injection | Best Use |
|---------|---------|-------------|-----------------|----------|
| **Panda PAU0B** | MediaTek MT7610U (`0E8D:7610`) | ✅ Linux | ✅ Linux | RPi 5 passive scanning |
| **Alfa AWUS036ACS** | RTL8811AU | ✅ Linux | ✅ Linux | Primary attack simulation adapter |

> Both adapters work best on native Linux. Android monitor mode requires custom kernel — not reliable for current phases.
> On LG Gram (WSL2 mirrored networking, Windows 11 Insiders): ADB works via `127.0.0.1` directly — no `usbipd-win` required. RPi 5 is the preferred node for adapter work.
> WSL2 kernel (6.6.x-microsoft) has `CONFIG_MT76x0U` disabled — PAU0B requires custom kernel build or RPi 5 for monitor mode. Driver: `mt76x0u` (in-kernel on native Linux, no DKMS needed).

---

## 📱 Mobile Devices

| Device | Role |
|--------|------|
| **OnePlus 10T** | Android phone available for development, not the archived device-evidence baseline |
| **Pixel 10 Pro XL** | High-end test target, latest Android |
| **Orbic RC400L** | Cellular threat detection node — runs Rayhunter (EFF) for IMSI catcher / rogue base station detection. Connected to desktop via USB; accessible at `http://192.168.1.1:8080` (WiFi) or `http://localhost:8080` via `adb forward tcp:8080 tcp:8080`. |

> **Motorola G4 Play 2024 and Revvl Tab 2 were stolen — removed from hardware inventory.**

---

## 💻 Compute

| Device | Boot Options | Use |
|--------|-------------|-----|
| **LG Gram 15 (15Z)** | Windows 11 Insiders (primary) + WSL2 Kali + Live USB Kali/ParrotOS | Electron desktop dev, Kali tooling, full native Linux via live USB when needed |
| **Raspberry Pi 5 (8GB)** | Kali Linux (microSD 1) + Ubuntu (microSD 2) | Dedicated monitor node, attack simulator lab, swap cards as needed |

> RPi 5 8GB has plenty of headroom to run Kismet, Wireshark, and wscanplus simultaneously.

---

## 🌐 Network Hardware

| Device | Type | Capabilities | Role |
|--------|------|-------------|------|
| **GL.iNet GL-MT300N (Mango)** | OpenWRT travel router, 2.4GHz, 300Mbps | VPN, repeater, captive portal, OpenWRT packages | Rogue AP simulation, captive portal testing |
| **GL.iNet Shadow (dual antenna)** | OpenWRT router, dual band | Evil twin setup, MITM lab, full OpenWRT | Primary network attack lab |

> Both GL.iNet devices run OpenWRT — ideal for simulating the exact attacks wscanplus is built to detect.

---

## 🔭 Cellular Detection — Orbic RC400L + Rayhunter

| Device | Firmware | Connection | Role |
|--------|----------|------------|------|
| **Orbic RC400L** | [Rayhunter (EFF)](https://github.com/EFForg/rayhunter) | USB (preferred) or WiFi | IMSI catcher / rogue base station detection via QMDL modem packet analysis |

### Rayhunter API

Rayhunter exposes an Axum (Rust) HTTP server on port 8080. All endpoints return JSON unless noted.

**Connect from desktop:**
```bash
# USB (preferred — no WiFi dependency)
adb forward tcp:8080 tcp:8080
# then access http://localhost:8080

# WiFi fallback
# http://192.168.1.1:8080
```

**Key endpoints for wscanplus integration:**

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/qmdl-manifest` | GET | List recordings; `current_entry` identifies active capture |
| `/api/analysis-report/{name}` | GET | Threat report for a recording |
| `/api/analysis` | GET | Analysis queue status (`queued`, `running`, `finished`) |
| `/api/system-stats` | GET | Battery, disk, memory, runtime metadata |
| `/api/start-recording` | POST | Begin QMDL capture |
| `/api/stop-recording` | POST | End QMDL capture |

**Analysis report structure:**
```json
{
  "packet_timestamp": "2026-04-16T21:00:00Z",
  "events": [
    {
      "event_type": { "type": "QualitativeWarning", "severity": "High" },
      "message": "Null cipher usage detected on NAS layer"
    }
  ]
}
```

**Detected threat categories:**
- IMSI capture requests
- Null cipher / encryption downgrade (2G, NAS, RRC layers)
- Forced 2G downgrade attacks
- Incomplete / malformed SIB transmissions
- Protocol anomalies in cellular signaling

**Severity levels:** `Informational` → `Low` → `Medium` → `High`

---



| Device | Firmware | Module | Capabilities |
|--------|----------|--------|-------------|
| **Flipper Zero** | Momentum | WiFi Dev Board (Marauder) | Evil twin, deauth flood, beacon spam, probe sniffing, PMKID capture, passive scanning, BLE |

### Marauder → wscanplus Detection Mapping

| Marauder Attack | wscanplus Detection Target |
|----------------|---------------------------|
| Deauth flood | Deauth storm alert |
| Evil twin / AP clone | Rogue AP detection |
| Beacon spam | Fake network flood detection |
| Probe sniffing | Passive recon detection |
| PMKID capture | WPA handshake grab alert |

### Known Device Signatures

| MAC | Firmware | Confidence | Notes |
|-----|----------|------------|-------|
| `DE:AD:BE:EF:FE:ED` | Momentum (Flipper Zero WiFi dev board) | HIGH | Default transmitter MAC set before every scan/attack. Configurable in Momentum (MAC, device name, and BLE name can all be changed); not all Flipper Zero firmware expose this setting. Any frame sourced from this MAC is a near-certain Flipper indicator for users who have not changed the default — treat as named device fingerprint, not generic spoofed MAC. |

> `DE:AD:BE:EF:FE:ED` ("deadbeeffeed") should trigger a HIGH confidence alert in wscanplus, distinct from generic locally-administered MAC warnings. Seed this into `BssidFingerprintEntity` as a known attack tool signature in Phase 4.

---

## 🧪 Recommended Test Lab Setup

```
RPi 5 (Kali microSD)
├── Panda PAU0B        → passive monitor mode scanning
├── Alfa AWUS036ACS    → active test injection
└── GL.iNet Shadow     → evil twin / rogue AP target

LG Gram (Win11 + WSL2 / Live USB Kali)  → Electron desktop dev
Flipper Zero (Momentum + Marauder)       → Pocket attack simulator
GL.iNet Mango                            → Captive portal / travel router simulation
OnePlus 10T / Pixel 10 Pro XL           → Android app primary test devices
Orbic RC400L (Rayhunter)                → Cellular / IMSI catcher detection
```

---

## ⚠️ Usage Policy

All hardware listed here is used exclusively for:
- Testing and validating wscanplus detection capabilities
- Simulating attacks in a private, controlled lab environment
- Personal defense research — understanding attacks in order to detect and alert on them

No hardware or techniques documented here are used offensively or against networks/devices without explicit authorization.

---

*Last updated: 2026-05-01*
