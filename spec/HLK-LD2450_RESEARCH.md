# HLK-LD2450 Radar Module Research Backup

This document backs up the current HLK-LD2450 research findings for wscan+ hardware/spec tracking.

## Verified Core Specs

- Module: HLK-LD2450 24 GHz FMCW mmWave radar.
- Function: motion target detection and tracking.
- Targets: up to 3 simultaneous targets.
- Reported telemetry: X coordinate, Y coordinate, angle, distance, speed, resolution/gate data.
- Supply: 5 V input.
- Supply capability: greater than 200 mA recommended.
- UART IO level: 3.3 V.
- UART default: 256000 baud, 8N1.
- Field of view: about ±60° azimuth and ±35° elevation.
- Range: about 6 m.
- Board size: about 15 mm × 44 mm.

## Pinout

| Pin | Function |
|---|---|
| 5V | 5 V power input |
| GND | Ground |
| TX | UART transmit from radar |
| RX | UART receive into radar |

Do not feed 5 V UART logic into RX. Treat serial IO as 3.3 V.

## UART Data Protocol

Radar target frames are binary, not text. Garbage-looking terminal output is expected unless decoded.

Known target frame markers:

```text
Header: AA FF 03 00
Tail:   55 CC
```

A standard frame is 30 bytes and contains three 8-byte target slots.

Each target slot contains little-endian values for X, Y, speed, and distance resolution/gate data.

## Coordinate / Speed Decoding Warning

The LD2450 uses a sign-bit style encoding, not normal signed two's-complement.

Observed rule from official protocol examples:

- If highest bit is 1: value is positive and should subtract `0x8000`.
- If highest bit is 0: value is negative and should be `0 - raw`.

Parser bug risk: treating X/Y/speed as normal int16 will mirror or corrupt target coordinates.

Example from protocol documentation:

```text
AA FF 03 00 0E 03 B1 86 10 00 40 01 ... 55 CC
```

Interpreted target 1:

- X raw `0x030E` = 782 -> `-782 mm`
- Y raw `0x86B1` = 34481 -> `1713 mm`
- Speed raw `0x0010` = 16 -> `-16 cm/s`
- Resolution raw `0x0140` = `320 mm`

## UART Command Frame Format

Configuration command frames use:

```text
Header: FD FC FB FA
Tail:   04 03 02 01
```

All configuration commands must be wrapped by enable/end configuration commands unless otherwise verified.

## Command Collection

### Enable Configuration

```text
FD FC FB FA 04 00 FF 00 01 00 04 03 02 01
```

Expected success ACK:

```text
FD FC FB FA 08 00 FF 01 00 00 01 00 40 00 04 03 02 01
```

### End Configuration

```text
FD FC FB FA 02 00 FE 00 04 03 02 01
```

### Single Target Mode

```text
FD FC FB FA 02 00 80 00 04 03 02 01
```

### Multi Target Mode

```text
FD FC FB FA 02 00 90 00 04 03 02 01
```

### Query Target Tracking Mode

```text
FD FC FB FA 02 00 91 00 04 03 02 01
```

### Read Firmware Version

```text
FD FC FB FA 02 00 A0 00 04 03 02 01
```

### Set Baud Rate

Command word: `0x00A1`

Known baud index values:

| Index | Baud |
|---|---:|
| `0x0001` | 9600 |
| `0x0002` | 19200 |
| `0x0003` | 38400 |
| `0x0004` | 57600 |
| `0x0005` | 115200 |
| `0x0006` | 230400 |
| `0x0007` | 256000 |
| `0x0008` | 460800 |

Default is `0x0007` / 256000.

Example setting 256000:

```text
FD FC FB FA 04 00 A1 00 07 00 04 03 02 01
```

### Factory Reset

Command word: `0x00A2`.

Known defaults after reset:

- Baud: 256000.
- Bluetooth: enabled.
- Tracking: multi-target.
- Area filtering: disabled.

### Reboot Module

```text
FD FC FB FA 02 00 A3 00 04 03 02 01
```

### Bluetooth On

```text
FD FC FB FA 04 00 A4 00 01 00 04 03 02 01
```

### Bluetooth Off

```text
FD FC FB FA 04 00 A4 00 00 00 04 03 02 01
```

### Get MAC Address

Command word: `0x00A5`.

### Query Area / Region Filtering

Command word: `0x00C1`.

### Set Area / Region Filtering

Command word: `0x00C2`.

Supported concepts:

- Disabled.
- Detect only inside configured zone.
- Ignore/filter inside configured zone.
- Up to three rectangular zones.
- Coordinates use LD2450 coordinate rules and little-endian values.

## BLE Findings

The LD2450 exposes a BLE UART-like transparent transport.

Known GATT layout:

| Purpose | UUID |
|---|---|
| Service | `0000FFF0-0000-1000-8000-00805F9B34FB` |
| Notify / sensor to client | `0000FFF1-0000-1000-8000-00805F9B34FB` |
| Write / client to sensor | `0000FFF2-0000-1000-8000-00805F9B34FB` |

Observed device naming format:

```text
HLK-LD2450_XXXX
```

Local observed device:

```text
HLK-LD2450_7B31
```

Practical notes:

- BLE is enabled by default.
- No pairing PIN was observed during phone testing.
- Official Hi-Link app connects directly.
- BLE supports live target viewing and configuration.
- Official app exposes OTA firmware updates.
- App wording observed: `2450 transparent transmission firmware`.
- BLE likely transports the same binary protocol as UART.

## Firmware / OTA Notes

Observed from user hardware/app screenshots:

- Installed firmware: `V2.04.23101915`.
- Offered firmware: `V2.14.25112412`.
- Official app performs firmware checks and OTA update workflow.

ESPHome documentation warns that LD2450 firmware should be `V2.02.23090617` or later for proper integration.

## User Hardware Validation

Validated directly by current hardware session:

| Capability | Status |
|---|---|
| UART wiring through FTDI to phone | verified |
| Binary UART stream present | verified |
| BLE app connection | verified |
| Official app live visualization | verified |
| Firmware query/update screen | verified |
| Multi-target visualization | verified |
| BLE device name observed | verified |
| UART and BLE used in same session | observed / likely workable |

## Orientation Notes

Community and screenshot references indicate orientation matters.

Recommended canonical orientation:

- Four larger antenna patches upward.
- Two smaller antenna patches downward.
- Maintain this orientation in enclosures and mounts.

Wrong orientation can skew zone geometry, angle interpretation, and coordinate mapping.

## ESPHome / Home Assistant Integration

ESPHome has an official `ld2450` component over UART.

Common entities/features:

- Presence binary sensor.
- Moving target binary sensor.
- Still target binary sensor.
- Target count.
- Moving target count.
- Still target count.
- Target 1/2/3 X coordinate.
- Target 1/2/3 Y coordinate.
- Target 1/2/3 speed.
- Target 1/2/3 angle.
- Target 1/2/3 distance.
- Target 1/2/3 resolution.
- Bluetooth enable/disable switch.
- Multi-target mode switch.
- Presence timeout setting.
- Zone coordinate numbers.
- Restart button.
- Factory reset button.
- Firmware version text sensor.
- MAC address text sensor.

Sample ESPHome wiring shape:

```yaml
uart:
  id: uart_ld2450
  tx_pin: GPIO17
  rx_pin: GPIO16
  baud_rate: 256000
  parity: NONE
  stop_bits: 1

ld2450:
  id: ld2450_radar
  uart_id: uart_ld2450
```

## Known Community Drivers / References To Track

- ESPHome official `ld2450` component.
- MassiPi `ld2450_ble` Home Assistant BLE custom integration.
- RBEGamer `HLK-LD2450` Arduino library.
- Home Assistant community LD2450 initial experiments thread.
- Home Assistant community full BLE LD2450 integration thread.
- Reddit / Arduino LD2450 wiring and baud discussions.
- Hi-Link official product page and Google Drive documentation bundle.

## Battery / Standalone Operation

Battery operation does not require an MCU if the chosen receiver is direct BLE.

Minimal direct deployment:

```text
Battery or 12 V source
  -> 5 V regulator
  -> LD2450
  -> BLE direct to Android dash / phone / tablet / Home Assistant BLE receiver
```

This is power-and-go after configuration when BLE range is adequate.

ESP relay deployment:

```text
Battery or 12 V source
  -> 5 V regulator
  -> LD2450 UART
  -> ESP32 / ESPHome node
  -> Wi-Fi / MQTT / Home Assistant / wscan+ bridge
```

Use an ESP node when:

- BLE range is poor.
- Local Home Assistant integration is desired.
- MQTT/WebSocket export is needed.
- Zone logic should run near the sensor.
- BLE should be disabled for exposure reduction.
- Android direct connection is not reliable in background.

Use direct Android BLE when:

- Sensor is close to Android dash/head unit/phone.
- A phone/tablet app can stay connected.
- Fast prototyping matters more than infrastructure.
- You want to avoid ESP firmware and Wi-Fi failure points.

## Power Notes

Official requirement is 5 V with more than 200 mA supply capability.

Community current estimate from HA discussion is around 120 mA average. User steady-state estimate from testing/expectation is closer to roughly 60–100 mA, excluding startup/OTA/BLE bursts.

Design recommendation:

- Size regulator for at least 500 mA.
- Use 1 A if using a cheap buck converter.
- Add local bulk capacitor near radar, e.g. 100–470 uF.
- Use clean 5 V from 12 V vehicle power through a buck converter.
- Account for vehicle spikes/noise if permanently installed.

Approximate steady power:

```text
5 V * 0.07 A = 0.35 W
5 V * 0.12 A = 0.60 W
```

12 V side through 85–92% buck is roughly tens of mA, not amps.

## Deployment Architectures

### Direct Android Dash Deployment

```text
12 V vehicle auxiliary / ACC
  -> fused 12 V feed
  -> 12 V to 5 V buck
  -> LD2450
  -> BLE
  -> Android dash / phone app
```

Best for rapid vehicle use.

### ESPHome Relay Deployment

```text
12 V or battery
  -> 5 V regulator
  -> LD2450 + ESP32
  -> UART at 256000 baud
  -> ESPHome API / MQTT
  -> Home Assistant / wscan+
```

Best for home automation or stable long-term telemetry.

### Standalone Portable BLE Node

```text
USB power bank / 18650 boost pack
  -> 5 V
  -> LD2450
  -> BLE to phone
```

Best for temporary placement, experiments, and mapping.

### Low-Power Gated Node

```text
Battery
  -> MCU-controlled load switch / MOSFET
  -> LD2450 power rail
  -> periodic wake/sense/report/sleep
```

Best for long battery life. Not needed for vehicle auxiliary battery use unless parked draw must be minimized.

## Security / Exposure Notes

- BLE is enabled by default.
- Device advertises identifiable `HLK-LD2450_XXXX` name.
- No PIN was observed.
- OTA path exists in official app.
- Consider disabling BLE for fixed installations when using UART/ESPHome.
- If BLE remains on, treat it as a local wireless management surface.

## Additional wscan+ Uses

Potential future integration ideas:

- Cabin occupancy detection.
- Rear cargo motion sensing.
- Motion-triggered RF capture.
- Physical-space context for Wi-Fi/BLE scans.
- Human movement correlation with network events.
- Sensor-fusion companion node.
- Indoor target map overlay.
- Vehicle parked-state intrusion detection.
- Room/desk/doorway zone automation.
- Motion-aware alerting through ntfy.sh or local notifications.
- Direct Android companion telemetry bridge.
- ESPHome-to-MQTT bridge into wscan+.

## Recommended Repo Follow-Up

- Add hardware manifest entry.
- Add `spec/HLK-LD2450_RESEARCH.md` as backed-up research.
- Later add parser test vectors from official example frames.
- Later add Android BLE capture notes after GATT enumeration.
- Later add ESPHome sample config under examples if project scope allows.
