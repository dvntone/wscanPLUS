# 2026-05-09 Pixel AVF rtl_tcp SDR Handoff

## Purpose

This note records the verified SDR architecture and diagnostics from the Pixel
10 Pro XL AVF Debian session. It should be used as the baseline for future
wscan+ SDR work on Android native Linux Terminal / AVF.

## Environment

- Device: Google Pixel 10 Pro XL
- Host: Android 16 Beta, native Linux Terminal / AVF Debian VM
- Guest: Debian 13 trixie
- Kernel: `6.12.74-android16-6-g91e49117396d-ab15177221-4k`
- SDR: Nooelec NESDR Mini 2, RTL2832U + R820T2
- Graphics: Weston/XWayland; GQRX and inspectrum run successfully
- Network: `enp0s13`, VM address observed as `10.46.107.175/24`
- Android rtl_tcp broker: `10.46.107.7:1234`

## Core Finding

Direct USB passthrough into Debian AVF is not available. Debian does not see
the RTL-SDR as a local USB device, and this should be treated as an AVF
architecture limitation rather than a driver, permission, OTG, or hardware
failure.

Working architecture:

```text
Nooelec NESDR Mini 2
  -> Android USB host access
  -> Android rtl_tcp server
  -> AVF Debian network client
  -> GQRX / capture / analysis tools
```

This matches the desired wscan+ direction: Android acts as the hardware/sensor
broker, while Debian/desktop handles analysis, visualization, tooling, and
future correlation.

## Android rtl_tcp Server

The Android server implementation in use is:

```text
https://github.com/signalwareltd/rtl_tcp_andro-
```

Observed Android launch profile:

```text
-a 10.46.107.7 -p 1234 -f 91800000 -s 1024000 -T 0
```

Meaning:

- Bind address: `10.46.107.7`
- Port: `1234`
- Initial frequency: `91.8 MHz`
- Initial sample rate: `1.024 MS/s`
- Bias tee: off

Important behavior:

- The server appears to be single-client.
- GQRX can hold the only active client connection.
- A second client may connect but fail to receive the `RTL0` header while GQRX
  is connected.
- Some client disconnects stop the Android server/service and require restart.
- Keeping the Android app visible in split screen may help prevent background
  throttling or app suspension.

## Confirmed Working

GQRX works with:

```text
rtl_tcp=10.46.107.7:1234
```

GQRX confirmed the server and tuner:

```text
The RTL TCP server reports a R820T tuner with 29 RF and 0 IF gains.
```

Raw capture works when using a GQRX-like profile and draining startup samples
before writing.

Known-good capture:

```bash
./sdr_iq_capture.sh 870952000 1600000 5 captures/gqrx_profile_870952k_1600ksps_settle1s.cu8
./sdr_iq_stats.py captures/gqrx_profile_870952k_1600ksps_settle1s.cu8
```

Observed good capture result:

```text
rtl_tcp header OK: tuner_type=5 gain_count=29
gain_mode=auto
settle_drained_bytes=5308416
captured_bytes=16056320
```

Observed good IQ stats:

```text
file_bytes=16056320
i_mean=127.378 q_mean=127.394
i_min=45 i_max=219
q_min=45 q_max=217
i_stddev=10.250 q_stddev=10.234
centered_rms=14.485
unique_byte_values=168
```

These stats are the current baseline for a valid raw RTL-SDR IQ capture in this
environment.

## Expected Failures

Do not spend time trying to make AVF Debian see the RTL-SDR directly unless that
is the explicit research question.

Expected-failure commands:

```bash
lsusb
usb-devices
ls /dev/bus/usb
rtl_test -t
rtl_tcp -a 0.0.0.0 -p 1234
```

Expected local RTL result:

```text
No supported devices found.
```

Classic local-device tools such as `rtl_fm` and `rtl_sdr` should not be assumed
to consume the Android broker directly. Prefer GQRX, GNU Radio `rtl_tcp`, tools
with explicit `rtl_tcp` support, or a dedicated network capture client.

## Failed / Fragile Tests

Early raw captures at `91.8 MHz / 1.024 MS/s`, `433.92 MHz / 1.024 MS/s`, and
`433.92 MHz / 2.048 MS/s` produced nearly flat IQ:

```text
i_min around 126, i_max around 129
q_min around 125/126, q_max around 129
i_stddev around 0.5
q_stddev around 0.5
unique_byte_values around 4-5
```

Those flat captures were not Debian networking failures. A later GQRX-profile
capture with a startup drain produced valid IQ.

A raw capture attempted while GQRX was connected timed out waiting for `RTL0`.
Socket state showed GQRX owned the single active connection. Treat this as a
single-client conflict, not SDR failure.

## Session Scripts

The session created local helper scripts in the AVF workspace:

- `sdr_diag.sh`
  - Passive by default.
  - `--live` explicitly connects to Android `rtl_tcp` and checks `RTL0`.
  - `--local-usb-probe` explicitly runs expected-failure USB/RTL probes.

- `sdr_iq_capture.sh`
  - Wrapper for raw CU8 IQ capture.
  - Defaults match the Android server profile: `91.8 MHz / 1.024 MS/s`.
  - Warns that client disconnect may stop the Android server.
  - Supports `RTL_TCP_GAIN`, `RTL_TCP_ANDROID_GAIN_PERCENT`,
    `RTL_TCP_NO_CONTROL=1`, and `RTL_TCP_SETTLE_SECONDS`.

- `sdr_rtl_tcp_capture.py`
  - Minimal `rtl_tcp` client.
  - Reads and validates `RTL0`.
  - Sends optional frequency, sample-rate, gain, and Android gain-percent
    commands.
  - Drains startup samples before writing output.

- `sdr_iq_stats.py`
  - Offline CU8 IQ sanity stats: mean, min/max, stddev, centered RMS, unique
    byte count, and byte histogram.

- `sdr_rtl433_listen.sh`
  - Runs `rtl_433` through `rtl_tcp:10.46.107.7:1234`.
  - Connected cleanly in a short test but decoded no packets during that window.

These helpers should be promoted into the repo only if wscan+ decides to keep a
Debian-side SDR utility layer.

## Current Operating Rules

- Ask before any live command that connects to `10.46.107.7:1234`.
- Run only one SDR client at a time.
- Close GQRX before raw capture.
- Restart the Android rtl_tcp server if a client disconnect stops the service.
- Avoid local USB debugging unless documenting AVF limitations.
- Prefer passive receive and analysis workflows.

## Known-Good Commands

Start GQRX:

```bash
gqrx
```

Use this GQRX device string:

```text
rtl_tcp=10.46.107.7:1234
```

Run passive diagnostic:

```bash
./sdr_diag.sh
```

Run live handshake diagnostic only when Android server is ready:

```bash
./sdr_diag.sh --live
```

Run known-good raw capture after closing GQRX:

```bash
./sdr_iq_capture.sh 870952000 1600000 5 captures/gqrx_profile_870952k_1600ksps_settle1s.cu8
./sdr_iq_stats.py captures/gqrx_profile_870952k_1600ksps_settle1s.cu8
```

Capture server default profile without sending control commands:

```bash
RTL_TCP_NO_CONTROL=1 ./sdr_iq_capture.sh 91800000 1024000 5 captures/server_default_fresh.cu8
./sdr_iq_stats.py captures/server_default_fresh.cu8
```

Test Android gain-percent extension:

```bash
RTL_TCP_ANDROID_GAIN_PERCENT=80 ./sdr_iq_capture.sh 433920000 1024000 5 captures/433M_android_gain80pct_1024ksps_5s.cu8
./sdr_iq_stats.py captures/433M_android_gain80pct_1024ksps_5s.cu8
```

## Future wscan+ Direction

The SDR path should be modeled as a distributed sensing bridge:

```text
Android Sensor Node
  - SDR acquisition
  - Wi-Fi sensing
  - BLE sensing
  - GPS/sensor telemetry
  - TCP/WebSocket/gRPC/VSOCK export

Debian/Desktop Analysis Layer
  - GQRX
  - inspectrum
  - GNU Radio
  - AI analysis
  - event correlation
  - visualization
```

Do not design future SDR work around direct AVF USB passthrough. Prefer network
or VM-bridge protocols between Android and the Debian/desktop analysis layer.
