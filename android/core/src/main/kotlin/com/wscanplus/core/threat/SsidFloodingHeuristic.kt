package com.wscanplus.core.threat

class SsidFloodingHeuristic : Heuristic {
    override val type: HeuristicType = HeuristicType.SSID_FLOODING

    override fun evaluate(
        input: ScanInput,
        context: ScanContext,
    ): ThreatSignal? {
        val first: ScanInput = context.currentResults.firstOrNull() ?: return null
        if (input !== first) return null

        val baseline: Int = context.baselineNetworkCount ?: return null
        val stdDev: Double = context.baselineStdDev ?: return null
        if (stdDev == 0.0) return null

        val currentCount: Int = context.currentResults.size
        val zScore: Double = (currentCount.toDouble() - baseline.toDouble()) / stdDev

        val confidence: Float = when {
            zScore >= 5.0 || currentCount > 200 -> 0.90f
            zScore >= 3.5 -> 0.70f
            zScore >= 2.5 -> 0.50f
            else -> return null
        }

        val zFormatted: String = "%.2f".format(zScore)

        return ThreatSignal(
            confidence = confidence,
            source = ThreatSource.LOCAL_HEURISTIC,
            reasons = listOf(
                "Abnormal network count: $currentCount (baseline: $baseline, z-score: $zFormatted)",
            ),
            heuristicType = type,
            bssid = "SCAN_LEVEL",
        )
    }
}
