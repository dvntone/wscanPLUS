package com.wscanplus.core.detector

import com.wscanplus.core.baseline.BaselineProfile
import com.wscanplus.core.baseline.TrustedApProfile
import com.wscanplus.core.evidence.ObservationEvent

private const val ANDROID_ONLY_CONFIDENCE_CAP = 0.75f

data class TrustedNetworkDriftResult(
    val observationId: String,
    val ssid: String,
    val bssid: String,
    val confidence: Float,
    val reasons: List<String>,
    val limitations: List<String>,
) {
    init {
        require(confidence in 0.0f..ANDROID_ONLY_CONFIDENCE_CAP) {
            "Confidence must be 0.0–$ANDROID_ONLY_CONFIDENCE_CAP, was $confidence"
        }
        require(reasons.isNotEmpty()) {
            "Trusted network drift result requires at least one reason"
        }
        require(reasons.size <= 3) {
            "Maximum 3 reasons, was ${reasons.size}"
        }
    }
}

private enum class TrustedNetworkDriftReason(
    val description: String,
) {
    UNTRUSTED_BSSID("Known SSID observed with untrusted BSSID"),
    UNEXPECTED_FREQUENCY("Trusted BSSID observed on unexpected frequency"),
    CAPABILITIES_CHANGED("Trusted BSSID capabilities changed from baseline"),
    RSSI_OUTSIDE_TOLERANCE("Trusted BSSID RSSI outside baseline tolerance"),
}

class TrustedNetworkDriftDetector {
    fun evaluate(
        observation: ObservationEvent,
        baseline: BaselineProfile,
    ): TrustedNetworkDriftResult? {
        val wifi = observation.wifi
        val sameSsidProfiles = baseline.trustedAccessPoints.filter { it.ssid == wifi.ssid }
        if (sameSsidProfiles.isEmpty()) return null

        val matchingBssidProfile =
            sameSsidProfiles.firstOrNull { it.bssid.equals(wifi.bssid, ignoreCase = true) }

        val driftReasons = mutableListOf<TrustedNetworkDriftReason>()

        if (matchingBssidProfile == null) {
            driftReasons += TrustedNetworkDriftReason.UNTRUSTED_BSSID
        } else {
            matchingBssidProfile.frequencyReason(wifi.frequencyMhz)?.let { driftReasons += it }
            matchingBssidProfile.capabilitiesReason(wifi.capabilities)?.let { driftReasons += it }
            matchingBssidProfile.rssiReason(wifi.rssiDbm)?.let { driftReasons += it }
        }

        val filteredReasons = driftReasons.take(3)
        if (filteredReasons.isEmpty()) return null

        return TrustedNetworkDriftResult(
            observationId = observation.id,
            ssid = wifi.ssid,
            bssid = wifi.bssid,
            confidence = confidenceFor(filteredReasons),
            reasons = filteredReasons.map { it.description },
            limitations =
                listOf(
                    "Android Wi-Fi observations are scan snapshots, not monitor-mode packet captures.",
                    "This detector reports baseline drift only; it does not confirm an evil twin or identify a responsible person or device owner.",
                ),
        )
    }

    private fun TrustedApProfile.frequencyReason(frequencyMhz: Int): TrustedNetworkDriftReason? =
        if (expectedFrequenciesMhz.isNotEmpty() && frequencyMhz !in expectedFrequenciesMhz) {
            TrustedNetworkDriftReason.UNEXPECTED_FREQUENCY
        } else {
            null
        }

    private fun TrustedApProfile.capabilitiesReason(capabilities: String): TrustedNetworkDriftReason? {
        if (expectedCapabilities.isEmpty()) return null

        val observedCapabilities = capabilities.normalizedCapabilityTokens()
        val expectedCapabilitySets = expectedCapabilities.map { it.normalizedCapabilityTokens() }

        return if (observedCapabilities !in expectedCapabilitySets) {
            TrustedNetworkDriftReason.CAPABILITIES_CHANGED
        } else {
            null
        }
    }

    private fun TrustedApProfile.rssiReason(rssiDbm: Int): TrustedNetworkDriftReason? {
        val median = rssiMedianDbm ?: return null
        val tolerance = rssiToleranceDb ?: return null
        return if (kotlin.math.abs(rssiDbm - median) > tolerance) {
            TrustedNetworkDriftReason.RSSI_OUTSIDE_TOLERANCE
        } else {
            null
        }
    }

    private fun confidenceFor(reasons: List<TrustedNetworkDriftReason>): Float =
        when {
            TrustedNetworkDriftReason.UNTRUSTED_BSSID in reasons -> 0.65f
            reasons.size >= 2 -> 0.60f
            else -> 0.45f
        }.coerceAtMost(ANDROID_ONLY_CONFIDENCE_CAP)
}

private fun String.normalizedCapabilityTokens(): Set<String> =
    Regex("\\[([^\\]]+)]")
        .findAll(this)
        .map { it.groupValues[1].trim().uppercase() }
        .filter { it.isNotEmpty() }
        .toSet()
