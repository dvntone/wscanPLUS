package com.wscanplus.core.threat

class EncryptionDowngradeHeuristic : Heuristic {
    override val type: HeuristicType = HeuristicType.ENCRYPTION_DOWNGRADE

    override fun evaluate(
        input: ScanInput,
        context: ScanContext,
    ): ThreatSignal? {
        val profile: BssidProfile = context.knownProfiles[input.bssid] ?: return null
        if (profile.observationCount < 3) return null

        val twoDaysMs: Long = 2L * 86_400_000L
        if (profile.lastSeenAt - profile.firstSeenAt < twoDaysMs) return null

        val currentSecurity: SecurityType = SecurityType.parse(input.capabilities)
        val historicalMax: SecurityType =
            profile.capabilitiesHistory
                .map { caps: String -> SecurityType.parse(caps) }
                .maxByOrNull { sec: SecurityType -> sec.rank }
                ?: return null

        if (currentSecurity.rank >= historicalMax.rank) return null

        val confidence: Float = downgradeConfidence(historicalMax.rank, currentSecurity.rank)
        val reason: String = "Security downgrade: $historicalMax \u2192 $currentSecurity"

        return ThreatSignal(
            confidence = confidence,
            source = ThreatSource.LOCAL_HEURISTIC,
            reasons = listOf(reason),
            heuristicType = type,
            bssid = input.bssid,
        )
    }

    private fun downgradeConfidence(
        historicalRank: Int,
        currentRank: Int,
    ): Float =
        when {
            historicalRank >= 3 && currentRank == 0 -> 0.85f
            historicalRank >= 3 && currentRank == 1 -> 0.75f
            historicalRank == 2 && currentRank == 0 -> 0.5f
            else -> 0.3f
        }
}
