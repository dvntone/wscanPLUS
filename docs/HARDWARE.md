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
| **Motorola G4 Play 2024** | Previous verified ADB/device-testing baseline — ADB over TCP via `adb connect 127.0.0.1:5555` (WSL2 mirrored networking, no usbipd required) |
| **Revvl Tab 2** | Current Android 15 active test target |

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

## 🐬 Flipper Zero

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
Motorola G4 Play / Revvl Tab 2          → Low-end + tablet UI testing
```

---

## ⚠️ Usage Policy

All hardware listed here is used exclusively for:
- Testing and validating wscanplus detection capabilities
- Simulating attacks in a private, controlled lab environment
- Personal defense research — understanding attacks in order to detect and alert on them

No hardware or techniques documented here are used offensively or against networks/devices without explicit authorization.

---

*Last updated: 2026-03-23*
