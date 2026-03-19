package com.wscanplus.core.threat

class EvilTwinHeuristic(
    private val vendorLookup: ((String) -> String?)? = null,
) : Heuristic {
    override val type: HeuristicType = HeuristicType.EVIL_TWIN

    override fun evaluate(
        input: ScanInput,
        context: ScanContext,
    ): ThreatSignal? {
        val peers: List<ScanInput> =
            context.currentResults.filter { other: ScanInput ->
                other.ssid == input.ssid && other.bssid != input.bssid
            }
        if (peers.isEmpty()) return null

        var best: ThreatSignal? = null
        for (peer: ScanInput in peers) {
            val signal: ThreatSignal? = evaluatePair(input, peer, context)
            if (signal != null && (best == null || signal.confidence > best.confidence)) {
                best = signal
            }
        }
        return best
    }

    private fun evaluatePair(
        input: ScanInput,
        peer: ScanInput,
        context: ScanContext,
    ): ThreatSignal? {
        if (shareHardwarePrefix(input.bssid, peer.bssid)) return null

        val scores: MutableList<Float> = mutableListOf()
        val reasons: MutableList<String> = mutableListOf()

        val ouiScore: Float = ouiMismatchScore(input.bssid, peer.bssid)
        if (ouiScore > 0f) {
            scores.add(ouiScore)
            reasons.add("OUI vendor mismatch")
        }

        val capScore: Float = capabilityMismatchScore(input.capabilities, peer.capabilities)
        if (capScore > 0f) {
            scores.add(capScore)
            reasons.add("Security capability mismatch")
        }

        val chanScore: Float = sameChannelScore(input.frequencyMhz, peer.frequencyMhz)
        if (chanScore > 0f) {
            scores.add(chanScore)
            reasons.add("Same channel")
        }

        val newBssidScore: Float = newBssidScore(input, peer, context)
        if (newBssidScore > 0f) {
            scores.add(newBssidScore)
            reasons.add("New BSSID for established SSID")
        }

        val rssiScore: Float = rssiAnomalyScore(input.rssiDbm, peer.rssiDbm)
        if (rssiScore > 0f) {
            scores.add(rssiScore)
            reasons.add("RSSI anomaly between twin APs")
        }

        if (scores.isEmpty()) return null

        val confidence: Float = combinedConfidence(scores)
        return ThreatSignal(
            confidence = confidence,
            source = ThreatSource.LOCAL_HEURISTIC,
            reasons = reasons.take(3),
            heuristicType = type,
            bssid = input.bssid,
        )
    }

    private fun shareHardwarePrefix(
        a: String,
        b: String,
    ): Boolean = a.length >= 14 && b.length >= 14 && a.substring(0, 14) == b.substring(0, 14)

    private fun ouiMismatchScore(
        bssidA: String,
        bssidB: String,
    ): Float {
        val lookup: ((String) -> String?) = vendorLookup ?: return 0f
        val ouiA: String = bssidA.take(8)
        val ouiB: String = bssidB.take(8)
        val vendorA: String = lookup(ouiA) ?: return 0f
        val vendorB: String = lookup(ouiB) ?: return 0f
        return if (vendorA != vendorB) 0.3f else 0f
    }

    private fun capabilityMismatchScore(
        capsA: String,
        capsB: String,
    ): Float {
        val secA: SecurityType = SecurityType.parse(capsA)
        val secB: SecurityType = SecurityType.parse(capsB)
        return if (secA != secB) 0.35f else 0f
    }

    private fun sameChannelScore(
        freqA: Int,
        freqB: Int,
    ): Float = if (freqA == freqB) 0.1f else 0f

    private fun newBssidScore(
        input: ScanInput,
        peer: ScanInput,
        context: ScanContext,
    ): Float {
        val inputKnown: Boolean = context.knownProfiles.containsKey(input.bssid)
        val peerKnown: Boolean = context.knownProfiles.containsKey(peer.bssid)
        return if (!inputKnown && peerKnown) 0.2f else 0f
    }

    private fun rssiAnomalyScore(
        rssiA: Int,
        rssiB: Int,
    ): Float {
        val diff: Int = kotlin.math.abs(rssiA - rssiB)
        return if (diff > 20) 0.15f else 0f
    }

    private fun combinedConfidence(scores: List<Float>): Float {
        var product = 1.0f
        for (s: Float in scores) {
            product *= (1f - s)
        }
        return (1f - product).coerceAtMost(0.95f)
    }
}
