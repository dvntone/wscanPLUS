package com.wscanplus.core.threat

class RssiAnomalyHeuristic : Heuristic {
    override val type: HeuristicType = HeuristicType.RSSI_ANOMALY

    override fun evaluate(
        input: ScanInput,
        context: ScanContext,
    ): ThreatSignal? {
        val threshold: Int =
            when (context.environmentType) {
                EnvironmentType.RESIDENTIAL -> -30
                EnvironmentType.OFFICE -> -25
                EnvironmentType.PUBLIC -> -20
                EnvironmentType.UNKNOWN -> -25
            }

        if (input.rssiDbm <= threshold) return null

        val isKnown: Boolean = context.knownProfiles.containsKey(input.bssid)

        val confidence: Float? =
            if (isKnown) {
                when {
                    input.rssiDbm > -20 -> 0.30f
                    input.rssiDbm > -25 -> 0.15f
                    else -> null
                }
            } else {
                when {
                    input.rssiDbm > -20 -> 0.80f
                    input.rssiDbm > -25 -> 0.60f
                    input.rssiDbm > -30 -> 0.45f
                    else -> null
                }
            }

        confidence ?: return null

        val knownLabel: String = if (isKnown) "known" else "new"
        val reason: String =
            "Unusually strong signal from $knownLabel network ${input.bssid}: ${input.rssiDbm} dBm " +
                "(threshold: $threshold dBm, env: ${context.environmentType})"

        return ThreatSignal(
            confidence = confidence,
            source = ThreatSource.LOCAL_HEURISTIC,
            reasons = listOf(reason),
            heuristicType = type,
            bssid = input.bssid,
        )
    }
}
