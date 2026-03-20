# Verification Status Index

**Date**: 2026-03-19 (America/Los_Angeles)

## Purpose
Make it explicit which docs are **verified by observed data**, which are **external specs**, and which are **policy/design guidance** (non?data).

## Claude Verification Note (Required)
- If any item here conflicts with Claude?s findings or new data, **Claude must re?verify** against the current device/app behavior and update the doc accordingly.

## Verified by Observed Data (ADB/Logcat/Exports)
- `09_adb_device_findings.md`
- `11_adb_expanded_findings.md`
- `12_logcat_wifi_findings.md`
- `13_wifi_toggle_logcat_capture.md`
- `14_wifi_toggle_ondevice_logcat.md`
- `15_location_gps_adb_findings.md`
- `16_adb_forward_reverse_test.md`
- `18_wifi_scan_off_behavior.md`
- `19_logcat_scanoff_snapshot.md`
- `20_logcat_scanoff_novpn.md`
- `21_altitude_location_findings.md`
- `22_altitude_stability_sampling.md`
- `25_mock_location_app_enabled.md`
- `26_mock_altitude_capture.md`
- `27_dev_build_adb_check.md`
- `28_dev_build_permissions_runtime.md`
- `29_bluetooth_state_adb.md`
- `30_dev_build_logcat_slice.md`
- `31_bluetooth_state_on.md`
- `32_dev_build_components.md`
- `33_wigle_export_schema.md`
- `34_wigle_m8b_export.md` (binary header observed; spec summarized separately)
- `35_bluetooth_location_off.md`
- `43_supplicant_reason_code_observations.md`

## Verified by External Specs / Source Repos
- `34_wigle_m8b_export.md` (format spec from WiGLE m8b repo)
- `44_wifiscanner_error_codes.md` (AOSP WifiScanner/WifiManager)
- `45_reason_codes_reference.md` (wpa_supplicant/hostapd ieee802_11_defs.h)
- `46_reason_code_frequency_ruleset.md` (reason meanings verified; rules are policy)

## Policy / Design Guidance (Not ?Data?)
- `01_data_exchange_contract.md`
- `02_integration_gaps.md`
- `03_desktop_hub_requirements.md`
- `04_heuristics_gap_analysis.md`
- `05_scan_throttling_and_power_strategy.md`
- `06_transport_format_tradeoffs.md`
- `07_android_disconnect_signals.md`
- `08_android_proactive_reactive_integration.md`
- `10_non_playstore_policy.md`
- `17_tether_loss_resilience.md`
- `23_altitude_mock_provider_attempt.md` (documented failure/constraint)
- `24_ble_data_status.md`
- `36_wigle_to_wscan_mapping.md`
- `37_adb_signal_matrix.md`
- `38_operational_checklist.md`
- `39_logcat_filter_cookbook.md`
- `40_privacy_boundaries.md`
- `41_evidence_anomaly_retention.md`
- `42_minimal_retention_schema.md`

## Notes
- ?Verified by Observed Data? means captured via ADB/logcat or device exports during this session.
- ?External Specs? means cross?checked against official source repositories.
- ?Policy/Design? means guidance or requirements, not observational data.
