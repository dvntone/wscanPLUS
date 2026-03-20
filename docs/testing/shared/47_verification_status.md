# Verification Status Index

**Date**: 2026-03-19 (America/Los_Angeles)

## Purpose
Make it explicit which docs are verified by observed data, which are external specs, and which are policy/design guidance (non-data).

## Claude Verification Note (Required)
- If any item here conflicts with current findings or new data, Claude must re-verify it against current device/app behavior and update the doc accordingly.

## Verified by Observed Data (ADB/Logcat/Exports)
- `docs/testing/devices/motorola-g4-play-2024/findings/09_adb_device_findings.md`
- `docs/testing/devices/motorola-g4-play-2024/findings/11_adb_expanded_findings.md`
- `docs/testing/devices/motorola-g4-play-2024/findings/12_logcat_wifi_findings.md`
- `docs/testing/devices/motorola-g4-play-2024/findings/13_wifi_toggle_logcat_capture.md`
- `docs/testing/devices/motorola-g4-play-2024/findings/14_wifi_toggle_ondevice_logcat.md`
- `docs/testing/devices/motorola-g4-play-2024/findings/15_location_gps_adb_findings.md`
- `docs/testing/devices/motorola-g4-play-2024/findings/16_adb_forward_reverse_test.md`
- `docs/testing/devices/motorola-g4-play-2024/findings/18_wifi_scan_off_behavior.md`
- `docs/testing/devices/motorola-g4-play-2024/findings/19_logcat_scanoff_snapshot.md`
- `docs/testing/devices/motorola-g4-play-2024/findings/20_logcat_scanoff_novpn.md`
- `docs/testing/devices/motorola-g4-play-2024/findings/21_altitude_location_findings.md`
- `docs/testing/devices/motorola-g4-play-2024/findings/22_altitude_stability_sampling.md`
- `docs/testing/devices/motorola-g4-play-2024/findings/25_mock_location_app_enabled.md`
- `docs/testing/devices/motorola-g4-play-2024/findings/26_mock_altitude_capture.md`
- `docs/testing/devices/motorola-g4-play-2024/findings/27_dev_build_adb_check.md`
- `docs/testing/devices/motorola-g4-play-2024/findings/28_dev_build_permissions_runtime.md`
- `docs/testing/devices/motorola-g4-play-2024/findings/29_bluetooth_state_adb.md`
- `docs/testing/devices/motorola-g4-play-2024/findings/30_dev_build_logcat_slice.md`
- `docs/testing/devices/motorola-g4-play-2024/findings/31_bluetooth_state_on.md`
- `docs/testing/devices/motorola-g4-play-2024/findings/32_dev_build_components.md`
- `docs/testing/devices/motorola-g4-play-2024/findings/33_wigle_export_schema.md`
- `docs/testing/devices/motorola-g4-play-2024/findings/34_wigle_m8b_export.md` (binary header observed; spec summarized separately)
- `docs/testing/devices/motorola-g4-play-2024/findings/35_bluetooth_location_off.md`
- `docs/testing/devices/motorola-g4-play-2024/findings/43_supplicant_reason_code_observations.md`

## Verified by External Specs / Source Repos
- `docs/testing/devices/motorola-g4-play-2024/findings/34_wigle_m8b_export.md` (format spec from WiGLE m8b repo)
- `docs/research/codex/44_wifiscanner_error_codes.md` (AOSP WifiScanner/WifiManager)
- `docs/research/codex/45_reason_codes_reference.md` (wpa_supplicant/hostapd ieee802_11_defs.h)
- `docs/research/codex/46_reason_code_frequency_ruleset.md` (reason meanings verified; rules are policy)

## Policy / Design Guidance (Not Data)
- `docs/research/codex/01_data_exchange_contract.md`
- `docs/research/codex/02_integration_gaps.md`
- `docs/research/codex/03_desktop_hub_requirements.md`
- `docs/research/codex/04_heuristics_gap_analysis.md`
- `docs/research/codex/05_scan_throttling_and_power_strategy.md`
- `docs/research/codex/06_transport_format_tradeoffs.md`
- `docs/research/codex/07_android_disconnect_signals.md`
- `docs/research/codex/08_android_proactive_reactive_integration.md`
- `docs/research/codex/10_non_playstore_policy.md`
- `docs/research/codex/17_tether_loss_resilience.md`
- `docs/research/codex/23_altitude_mock_provider_attempt.md` (documented failure/constraint)
- `docs/research/codex/24_ble_data_status.md`
- `docs/research/codex/36_wigle_to_wscan_mapping.md`
- `docs/testing/shared/37_adb_signal_matrix.md`
- `docs/testing/shared/38_operational_checklist.md`
- `docs/testing/shared/39_logcat_filter_cookbook.md`
- `docs/research/codex/40_privacy_boundaries.md`
- `docs/research/codex/41_evidence_anomaly_retention.md`
- `docs/research/codex/42_minimal_retention_schema.md`

## Notes
- "Verified by Observed Data" means captured via ADB/logcat or device exports during this session.
- "External Specs" means cross-checked against official source repositories.
- "Policy/Design" means guidance or requirements, not observational data.
