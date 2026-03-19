package com.wscanplus.core.threat

private const val THIRTY_MINUTES_MS: Long = 30 * 60 * 1000L
private const val MIN_OBSERVATION_COUNT: Int = 5

class BssidFingerprintHeuristic(
    private val ouiLookup: ((String) -> String?)?,
) : Heuristic {
    override val type: HeuristicType = HeuristicType.BSSID_FINGERPRINT

    override fun evaluate(
        input: ScanInput,
        context: ScanContext,
    ): ThreatSignal? {
        val subSignals: MutableList<Float> = mutableListOf()
        val reasons: MutableList<String> = mutableListOf()

        // Sub-signal 1: Locally-administered MAC
        val firstByte: Int =
            input.bssid
                .split(":")
                .firstOrNull()
                ?.toIntOrNull(16) ?: 0
        if ((firstByte and 0x02) != 0) {
            subSignals.add(0.25f)
            reasons.add("Locally-administered MAC address: ${input.bssid}")
        }

        // Sub-signal 2: BSSID rotation detection
        var rotationFired: Boolean = false
        var oldBssid: String? = null
        if (!context.knownProfiles.containsKey(input.bssid)) {
            val matchingProfile: BssidProfile? =
                context.knownProfiles.values.firstOrNull { profile: BssidProfile ->
                    profile.ssids.contains(input.ssid) &&
                        profile.observationCount >= MIN_OBSERVATION_COUNT &&
                        (input.timestamp - profile.lastSeenAt) <= THIRTY_MINUTES_MS
                }
            if (matchingProfile != null) {
                rotationFired = true
                oldBssid = matchingProfile.bssid
                subSignals.add(0.35f)
                reasons.add(
                    "BSSID rotated for SSID \"${input.ssid}\": was ${matchingProfile.bssid}, now ${input.bssid}",
                )
            }
        }

        // Sub-signal 3: OUI vendor mismatch (only when ouiLookup is non-null AND rotation fired)
        if (ouiLookup != null && rotationFired && oldBssid != null && reasons.size < 3) {
            val newVendor: String? = ouiLookup.invoke(input.bssid)
            val oldVendor: String? = ouiLookup.invoke(oldBssid)
            val vendorSignal: Float =
                if (newVendor != null && oldVendor != null && newVendor == oldVendor) {
                    reasons.add("OUI vendor unchanged after rotation: $newVendor")
                    0.15f
                } else {
                    reasons.add(
                        "OUI vendor mismatch after rotation: $oldVendor -> $newVendor",
                    )
                    0.40f
                }
            subSignals.add(vendorSignal)
        }

        if (subSignals.isEmpty()) return null

        val combined: Float =
            subSignals
                .fold(1.0f) { acc: Float, signal: Float ->
                    acc * (1.0f - signal)
                }.let { remaining: Float -> (1.0f - remaining).coerceAtMost(0.95f) }

        return ThreatSignal(
            confidence = combined,
            source = ThreatSource.LOCAL_HEURISTIC,
            reasons = reasons.take(3),
            heuristicType = type,
            bssid = input.bssid,
        )
    }
}
