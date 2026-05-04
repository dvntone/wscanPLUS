package com.wscanplus.core.threat

data class BssidProfile(
    val bssid: String,
    val firstSeenAt: Long,
    val lastSeenAt: Long,
    val ssids: Set<String>,
    val capabilitiesHistory: List<String>,
    val observationCount: Int,
    val ouiVendor: String?,
)

enum class EnvironmentType { RESIDENTIAL, OFFICE, PUBLIC, UNKNOWN }

data class ScanInput(
    val bssid: String,
    val ssid: String,
    val isHidden: Boolean,
    val capabilities: String,
    val rssiDbm: Int,
    val frequencyMhz: Int,
    val channelWidth: Int,
    val timestamp: Long,
)

data class ScanContext(
    val currentResults: List<ScanInput>,
    val knownProfiles: Map<String, BssidProfile>,
    val baselineNetworkCount: Int?,
    val baselineStdDev: Double?,
    val environmentType: EnvironmentType,
)
