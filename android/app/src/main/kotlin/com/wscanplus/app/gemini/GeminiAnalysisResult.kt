package com.wscanplus.app.gemini

/**
 * Result of a [GeminiThreatAnalyzer.analyze] call.
 *
 * - [Success]         — Gemini returned a narrative for the detected threats.
 * - [NoThreats]       — Signal list was empty; no analysis performed.
 * - [ConsentRequired] — User has not granted consent for cloud analysis.
 * - [Unavailable]     — Network failure, quota exhausted, or SDK error.
 */
sealed class GeminiAnalysisResult {
    data class Success(
        val narrative: String,
    ) : GeminiAnalysisResult()

    object NoThreats : GeminiAnalysisResult()

    object ConsentRequired : GeminiAnalysisResult()

    object Unavailable : GeminiAnalysisResult()
}
