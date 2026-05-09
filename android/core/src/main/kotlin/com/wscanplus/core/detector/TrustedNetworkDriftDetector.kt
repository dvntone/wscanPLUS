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

        val reasons = mutableListOf<String>()

        if (matchingBssidProfile == null) {
            reasons += "Known SSID observed with untrusted BSSID"
        } else {
            reasons += matchingBssidProfile.frequencyReason(wifi.frequencyMhz)
            reasons += matchingBssidProfile.capabilitiesReason(wifi.capabilities)
            reasons += matchingBssidProfile.rssiReason(wifi.rssiDbm)
        }

        val filteredReasons = reasons.filterNotNull().take(3)
        if (filteredReasons.isEmpty()) return null

        return TrustedNetworkDriftResult(
            observationId = observation.id,
            ssid = wifi.ssid,
            bssid = wifi.bssid,
            confidence = confidenceFor(filteredReasons),
            reasons = filteredReasons,
            limitations = listOf(
                "Android Wi-Fi observations are scan snapshots, not monitor-mode packet captures.",
                "This detector reports baseline drift only; it does not confirm an evil twin or identify an attacker.",
            ),
        )
    }

    private fun TrustedApProfile.frequencyReason(frequencyMhz: Int): String? =
        if (expectedFrequenciesMhz.isNotEmpty() && frequencyMhz !in expectedFrequenciesMhz) {
            "Trusted BSSID observed on unexpected frequency"
        } else {
            null
        }

    private fun TrustedApProfile.capabilitiesReason(capabilities: String): String? =
        if (expectedCapabilities.isNotEmpty() && capabilities !in expectedCapabilities) {
            "Trusted BSSID capabilities changed from baseline"
        } else {
            null
        }

    private fun TrustedApProfile.rssiReason(rssiDbm: Int): String? {
        val median = rssiMedianDbm ?: return null
        val tolerance = rssiToleranceDb ?: return null
        return if (kotlin.math.abs(rssiDbm - median) > tolerance) {
            "Trusted BSSID RSSI outside baseline tolerance"
        } else {
            null
        }
    }

    private fun confidenceFor(reasons: List<String>): Float =
        when {
            reasons.any { it.contains("untrusted BSSID") } -> 0.65f
            reasons.size >= 2 -> 0.60f
            else -> 0.45f
        }.coerceAtMost(ANDROID_ONLY_CONFIDENCE_CAP)
}
