package com.wscanplus.core.threat

class HeuristicEngine(
    private val heuristics: List<Heuristic>,
) {
    fun analyze(context: ScanContext): List<ThreatSignal> =
        context.currentResults.flatMap { input: ScanInput ->
            heuristics.mapNotNull { heuristic: Heuristic ->
                heuristic.evaluate(input, context)
            }
        }
}
