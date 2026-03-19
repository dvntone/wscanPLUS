package com.wscanplus.core.threat

class KarmaHeuristic : Heuristic {
    override val type: HeuristicType = HeuristicType.KARMA_ATTACK

    override fun evaluate(
        input: ScanInput,
        context: ScanContext,
    ): ThreatSignal? {
        val group: List<ScanInput> = context.currentResults.filter { entry: ScanInput ->
            entry.bssid == input.bssid
        }

        val first: ScanInput = group.firstOrNull() ?: return null
        if (input !== first) return null

        val distinctSsids: Set<String> = group
            .filter { entry: ScanInput -> !entry.isHidden }
            .map { entry: ScanInput -> entry.ssid }
            .toSet()

        val count: Int = distinctSsids.size
        if (count < 3) return null

        val baseConfidence: Float = when {
            count >= 10 -> 0.95f
            count >= 5 -> 0.75f
            else -> 0.45f
        }

        val reasons: MutableList<String> = mutableListOf(
            "BSSID ${input.bssid} broadcasting $count distinct SSIDs",
        )

        val frequencies: Set<Int> = group.map { entry: ScanInput -> entry.frequencyMhz }.toSet()
        val rssiValues: List<Int> = group.map { entry: ScanInput -> entry.rssiDbm }
        val rssiRange: Int = (rssiValues.max() - rssiValues.min())
        val hasBonus: Boolean = frequencies.size == 1 && rssiRange <= 6

        val confidence: Float = if (hasBonus) {
            reasons.add("Same channel + tight RSSI cluster")
            (baseConfidence + 0.1f).coerceAtMost(0.95f)
        } else {
            baseConfidence
        }

        return ThreatSignal(
            confidence = confidence,
            source = ThreatSource.LOCAL_HEURISTIC,
            reasons = reasons,
            heuristicType = type,
            bssid = input.bssid,
        )
    }
}
