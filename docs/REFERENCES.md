# wscan+ Reference Repositories

Reference projects used to inform wscanplus architecture, features, code quality, and detection logic.
Agents should study these before implementing related features.

---

## ⭐ Gold Standard

| Repo | URL | Why |
|------|-----|-----|
| TrafficDetector | https://github.com/rajkunamaneni/TrafficDetector | Primary code quality and architecture bar. Python + Snort + tshark + Nmap on RPi. Detects MITM/eavesdropping, delivers plain-language alerts. Clean, well-structured code. Study before writing any detection logic. |

---

## ⭐ Critical Android Companion Reference

| Repo | URL | Why |
|------|-----|-----|
| wigle-wifi-wardriving | https://github.com/wiglenet/wigle-wifi-wardriving | Primary feature reference for the Android companion app. Covers scan history, network mapping, signal strength visualization, background scanning, and data export — all features wscanplus must implement. NOTE: WiGLE as a cloud service is not used; the Android app UX and feature set is the reference. |

---

## Attack Pattern References (what wscanplus must detect)

| Repo | URL | Why |
|------|-----|-----|
| ESP32Marauder | https://github.com/justcallmekoko/ESP32Marauder | WiFi/BT offensive + defensive tools — deauth flood, evil twin, beacon spam, PMKID capture, probe sniffing. Primary attack simulation tool for testing detection. |
| evilginx2 | https://github.com/kgretzky/evilginx2 | Go MITM framework — phishing + session cookie capture + 2FA bypass via reverse proxy. Reference for MITM detection rules. |
| wifi-pineapple-cloner | https://github.com/xchwarze/wifi-pineapple-cloner | WiFi Pineapple on generic OpenWrt hardware — rogue AP, evil twin, MITM. Attack pattern reference. |
| wifite2 | https://github.com/derv82/wifite2 | Python — automated WiFi auditing (WPS Pixie-Dust, WPA handshake, PMKID, WEP). Detection rule reference. |
| rpi-hunter | https://github.com/BusesCanFly/rpi-hunter | Python — network discovery via arp-scan + tshark. Recon patterns to detect on LAN. |

---

## Desktop App References

| Repo | URL | Why |
|------|-----|-----|
| sniffnet | https://github.com/GyulyVGC/sniffnet | Rust + Iced — cross-platform network monitor GUI, real-time charts, 6000+ service identification, PCAP import/export. Desktop UI and feature reference. |
| bettercap/caplets | https://github.com/bettercap/caplets | BetterCap caplets + proxy modules. BetterCap integration reference. |
| mitmproxy | https://github.com/mitmproxy/mitmproxy | TLS-capable intercepting HTTP proxy. MITM detection testing and proxy reference. |
| NAVV (CISA) | https://github.com/cisagov/network-architecture-verification-and-validation | Python + Zeek — PCAP → structured reports. Government-grade PCAP analysis pipeline reference. |
| Hoppscotch | https://github.com/hoppscotch/hoppscotch | Open-source API development ecosystem. API testing and UI reference. |

---

## Android Architecture References

| Repo | URL | Why |
|------|-----|-----|
| ProtonVPN android-app | https://github.com/ProtonVPN/android-app | Production Kotlin VPN app — architecture, permissions model, kill switch, background service patterns. |
| WGTunnel | https://github.com/wgtunnel/android | WireGuard/AmneziaWG Android — VPN tethering and network interface reference. |
| ByeDPIAndroid | https://github.com/krlvm/ByeDPIAndroid | Local VPN service for network interception — network layer reference. |
| Dhizuku | https://github.com/iamr0s/Dhizuku | DeviceOwner permission sharing — Android privilege model reference. |
| ya-webadb | https://github.com/yume-chan/ya-webadb | Browser-based ADB interface — skip for MVP (adbkit is sufficient); revisit only if web dashboard is added. |
| MetaRadar | https://github.com/BLE-Research-Group/MetaRadar | BLE environment monitoring — wireless scanning patterns for Android. |
| ReTerminal | https://github.com/RohitKushvaha01/ReTerminal | Android terminal emulator — terminal UI reference for advanced mode. |

---

## Linux / System References

| Repo | URL | Why |
|------|-----|-----|
| Amnezia VPN Client | https://github.com/amnezia-vpn/amnezia-client | Desktop + mobile VPN client (C++) — cross-platform architecture reference. |
| Sysd-Manager | https://github.com/plrigaux/sysd-manager | systemd unit GUI — Linux service management UI patterns for desktop app. |

---

## Desktop / Node.js References

| Library | URL | Why |
|---------|-----|-----|
| adbkit | https://github.com/openstf/adbkit | Pure Node.js ADB client — used on desktop side for device enumeration, port forwarding, and multi-device management. Primary ADB transport library for wscanplus. |

---

## Do NOT Use

| Repo | Reason |
|------|--------|
| SonarQube Scan Action | Explicitly excluded — no CodeQL/Sonar/Semgrep/Detekt per AGENTS.md |
| ipdrone / Xteam / TermuxCyberArmy | Offensive-only tools with no defensive relevance to wscanplus |
| awesome-shizuku / ShizuWall | Shizuku dropped from wscanplus scope — adds zero scanning capability, no monitor mode, throttling still applies |
| nexmon / seemoo-lab/nexmon | Broadcom-only chipset requirement (~20% of market); incompatible with primary test devices (OnePlus 10T, Pixel 10 Pro XL). Requires per-device firmware flashing — not viable for mainstream app distribution. |
