package com.wscanplus.app.gemini

import com.wscanplus.core.threat.ThreatSignal

/**
 * Builds a structured Gemini prompt from a list of [ThreatSignal] objects.
 *
 * Privacy note: raw BSSIDs are not included in the prompt — only heuristic
 * type, confidence, and reason strings are sent to the cloud model.
 */
object ThreatPromptBuilder {
    fun build(signals: List<ThreatSignal>): String {
        val sb = StringBuilder()
        sb.appendLine(
            "You are a WiFi security analyst. Analyze the following threat signals " +
                "detected by a mobile WiFi scanner. Provide a concise plain-English summary " +
                "(3–5 sentences) covering: what was detected, the likely threat, and what the " +
                "user should do. Assume a non-technical audience.",
        )
        sb.appendLine()
        sb.appendLine("Detected signals (${signals.size}):")
        signals.forEachIndexed { index, signal ->
            val type =
                signal.heuristicType
                    ?.name
                    ?.replace('_', ' ')
                    ?.lowercase()
                    ?.replaceFirstChar { it.uppercase() }
                    ?: signal.source.name
            sb.appendLine("${index + 1}. $type — confidence ${"%.0f".format(signal.confidence * 100)}%")
            signal.reasons.forEach { reason -> sb.appendLine("   • $reason") }
        }
        sb.appendLine()
        sb.append("Provide your threat assessment:")
        return sb.toString()
    }
}
