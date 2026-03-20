# Codex Findings & Gaps (Index)

This folder contains Codex-generated, **verified** findings, ADB test results, and integration gaps meant to help Claude and dvntone ship a working Linux desktop app + Android companion.

## Contents
- `01_data_exchange_contract.md` ? Proposed device?desktop data contract, including message types and stability requirements.
- `02_integration_gaps.md` ? Concrete gaps in current repo docs and what is missing for a deployable system.
- `03_desktop_hub_requirements.md` ? Desktop-side requirements to support multi-device and offline operation.
- `04_heuristics_gap_analysis.md` ? Kismet alert classes not yet mapped into WSCAN+ heuristics.
- `05_scan_throttling_and_power_strategy.md` ? Scan throttling and duty?cycle guidance to reduce battery drain.
- `06_transport_format_tradeoffs.md` ? JSON vs protobuf vs flatbuffer tradeoffs for device?desktop.
- `07_android_disconnect_signals.md` ? Verified Android disconnect/error signals that can indicate attack.
- `08_android_proactive_reactive_integration.md` ? Proactive vs reactive pipeline and handoff triggers.
- `09_adb_device_findings.md` ? Baseline device OS/SDK and Wi?Fi subsystem findings.
- `10_non_playstore_policy.md` ? Non?Play?Store policy note (accepted constraint).
- `11_adb_expanded_findings.md` ? Expanded ADB checks (wifi, connectivity, scan stats).
- `12_logcat_wifi_findings.md` ? Logcat filters that produced Wi?Fi related signals.
- `13_wifi_toggle_logcat_capture.md` ? Logcat capture during Wi?Fi toggle test (host?side).
- `14_wifi_toggle_ondevice_logcat.md` ? On?device logcat capture with ADB Wi?Fi toggle.
- `15_location_gps_adb_findings.md` ? Location/GPS settings and provider state.
- `16_adb_forward_reverse_test.md` ? Verified ADB forward/reverse works.
- `17_tether_loss_resilience.md` ? Recovery steps if tether/network drops during ADB testing.
- `18_wifi_scan_off_behavior.md` ? Verified behavior of scan results when Wi?Fi is OFF vs scan?always ON/OFF.
- `19_logcat_scanoff_snapshot.md` ? Logcat snapshot while Wi?Fi OFF and scan?always ON (VPN on).
- `20_logcat_scanoff_novpn.md` ? Logcat snapshot while Wi?Fi OFF and scan?always ON (VPN off).
- `21_altitude_location_findings.md` ? Verified altitude/vertical accuracy fields from dumpsys location.
- `22_altitude_stability_sampling.md` ? Altitude sampling over time (stale data observed).
- `23_altitude_mock_provider_attempt.md` ? ADB mock?provider attempt (blocked by permissions).
- `24_ble_data_status.md` ? BLE data status (not yet captured).
- `25_mock_location_app_enabled.md` ? FakeGPS Route set as mock?location app.
- `26_mock_altitude_capture.md` ? Mock?app altitude injected and marked as `mock`.
- `27_dev_build_adb_check.md` ? Dev build package info + launch/logcat check.
- `28_dev_build_permissions_runtime.md` ? Dev build permissions/appops/foreground service state.
- `29_bluetooth_state_adb.md` ? Bluetooth state (OFF) and BLE apps registered (0).
- `30_dev_build_logcat_slice.md` ? Minimal logcat slice, no app?emitted logs.
- `31_bluetooth_state_on.md` ? Bluetooth state (ON) with BLE stack initialized.
- `32_dev_build_components.md` ? Declared activities/receivers/providers via dumpsys.
- `33_wigle_export_schema.md` ? WiGLE CSV/KML export schema (structure only).
- `34_wigle_m8b_export.md` ? WiGLE .m8b export is binary; header identifiers recorded and spec summarized.
- `35_bluetooth_location_off.md` ? Bluetooth + location disabled via ADB.
- `36_wigle_to_wscan_mapping.md` ? WiGLE export ? WSCAN+ field mapping.
- `37_adb_signal_matrix.md` ? ADB signal matrix (what?s observable vs requires app code).
- `38_operational_checklist.md` ? Reproducible test checklist.
- `39_logcat_filter_cookbook.md` ? Logcat filters and what they yield.
- `40_privacy_boundaries.md` ? Sensitive data handling guidance.
- `41_evidence_anomaly_retention.md` ? Evidence/anomaly retention policy (timeframes, RSSI, MACs, geo).
- `42_minimal_retention_schema.md` ? Minimal retention schema with FP?safe alert gating.
- `43_supplicant_reason_code_observations.md` ? Observed deauth/disconnect reason codes in logcat.
- `44_wifiscanner_error_codes.md` ? WifiScanner scan failure reason codes vs disconnect reasons.
- `45_reason_codes_reference.md` ? Verified 802.11 reason code reference; status vs reason clarification.
- `46_reason_code_frequency_ruleset.md` ? Frequency?based anomaly rules using verified reason meanings only.
- `47_verification_status.md` ? Explicit verification status (observed vs spec vs policy).
- `48_claude_briefing.md` ? Claude handoff summary (non?coding).
- `49_data_flow_diagram.md` ? Conceptual device?desktop data flow diagram.
- `50_one_page_summary.md` ? One?page summary (Markdown).
- `50_one_page_summary.pdf` ? One?page summary (PDF).
- `51_app_code_review_suggestions.md` ? App code review suggestions (verified).
- `52_app_logging_checklist.md` ? Minimal app logging checklist for verification.
- `53_app_code_review_verified.md` ? Verified improvements based on observed behavior.

## Raw Logs / Artifacts
- `wscan_toggle_logcat.txt` ? Host-side logcat capture during Wi?Fi toggle.
- `wscan_disconnect_logcat.txt` ? Extended capture around Wi?Fi disconnect/reconnect tests.
- `wscan_scanoff_logcat_20260319_080549.txt` ? Logcat snapshot while Wi?Fi OFF + scan?always ON (VPN on).
- `wscan_scanoff_novpn_logcat_20260319_080738.txt` ? Logcat snapshot while Wi?Fi OFF + scan?always ON (VPN off).
