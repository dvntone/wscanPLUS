package com.wscanplus.core.threat

data class PolicyConfig(
    val minimumConfidence: Float = 0.3f,
    val falsePositiveBrakes: Boolean = true,
)

class PolicyGate(
    private val config: PolicyConfig = PolicyConfig(),
) {
    fun filter(signals: List<ThreatSignal>): List<ThreatSignal> =
        signals.filter { signal: ThreatSignal ->
            signal.confidence >= config.minimumConfidence
        }
}
