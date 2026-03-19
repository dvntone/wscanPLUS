package com.wscanplus.core.threat

class WepOpenHeuristic : Heuristic {
    override val type: HeuristicType = HeuristicType.WEP_OPEN

    override fun evaluate(
        input: ScanInput,
        context: ScanContext,
    ): ThreatSignal? {
        val security: SecurityType = SecurityType.parse(input.capabilities)
        val confidence: Float =
            when (security) {
                SecurityType.OPEN -> 0.4f
                SecurityType.WEP -> 0.6f
                SecurityType.WPA -> 0.2f
                SecurityType.WPA2, SecurityType.OWE, SecurityType.WPA3 -> return null
            }
        return ThreatSignal(
            confidence = confidence,
            source = ThreatSource.LOCAL_HEURISTIC,
            reasons = listOf(reasonFor(security)),
            heuristicType = type,
            bssid = input.bssid,
        )
    }

    private fun reasonFor(security: SecurityType): String =
        when (security) {
            SecurityType.OPEN -> "Open network — no encryption"
            SecurityType.WEP -> "WEP encryption — trivially crackable"
            SecurityType.WPA -> "WPA (original) — deprecated, known vulnerabilities"
            else -> ""
        }
}
