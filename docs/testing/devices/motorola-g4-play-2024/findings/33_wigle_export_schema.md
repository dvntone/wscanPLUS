# WiGLE Export Schema (ADB Inspection)

**Date**: 2026-03-19 (America/Los_Angeles)

## Source Files (shared storage)
Found in `/sdcard/Documents`:
- `WigleWifi_20260319083917.csv.gz`
- `WigleWifi_20260319083944.kml`
- `WigleWifi_20260319083955.csv.gz`
- `WigleWifi_20260319084006.kml`

## CSV Schema (from gzip header)
Command used:
```powershell
adb shell "gzip -cd /sdcard/Documents/WigleWifi_20260319083917.csv.gz | head -n 2"
```

Header fields:
- `MAC`
- `SSID`
- `AuthMode`
- `FirstSeen`
- `Channel`
- `RSSI`
- `CurrentLatitude`
- `CurrentLongitude`
- `AltitudeMeters`
- `AccuracyMeters`
- `Type`

## KML Structure (high?level)
Command used:
```powershell
adb shell "head -n 20 /sdcard/Documents/WigleWifi_20260319083944.kml"
```

Observed structure:
- Standard KML with a `<Folder>` named **Wifi Networks**.
- Each `<Placemark>` includes:
  - SSID (name)
  - Network ID (BSSID/MAC)
  - Capabilities / encryption
  - Frequency / channel
  - Timestamp and formatted time
  - Signal (RSSI)
  - Type
  - Coordinates (lon,lat)

## Notes
- The exported files include **sensitive identifiers and location coordinates**. Only schema/structure was recorded here.
- This format maps cleanly to the WSCAN+ data contract (BSSID, SSID, RSSI, channel, time, lat/long, altitude).
