package com.wscanplus.core.threat

interface Heuristic {
    val type: HeuristicType

    fun evaluate(
        input: ScanInput,
        context: ScanContext,
    ): ThreatSignal?
}
