package com.wscanplus.app.gemini

import android.util.Log
import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.type.GenerativeBackend
import com.wscanplus.app.privacy.ConsentReader
import com.wscanplus.core.threat.ThreatSignal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Calls Firebase AI Logic (Gemini) to produce a plain-English threat narrative
 * from a list of [ThreatSignal] objects detected by the local heuristic engine.
 *
 * - Consent-gated: returns [GeminiAnalysisResult.ConsentRequired] if the user
 *   has not opted in to cloud analysis.
 * - Main-safe: all network I/O is dispatched to [Dispatchers.IO].
 * - Model: [MODEL_NAME] — non-EOL stable model as of 2026-04-01.
 *   Update if this model is deprecated; do NOT use gemini-2.0-flash (EOL June 2026).
 *
 * The returned narrative is suitable for display to a non-technical user.
 * Persistence and UI are handled in follow-on Phase 4 PRs.
 */
class GeminiThreatAnalyzer(
    private val consentReader: ConsentReader,
) {
    private val model by lazy {
        FirebaseAI
            .getInstance(backend = GenerativeBackend.googleAI())
            .generativeModel(MODEL_NAME)
    }

    suspend fun analyze(signals: List<ThreatSignal>): GeminiAnalysisResult {
        if (!consentReader.isConsentGiven()) {
            Log.d(TAG, "Gemini analysis skipped — consent not granted")
            return GeminiAnalysisResult.ConsentRequired
        }
        if (signals.isEmpty()) return GeminiAnalysisResult.NoThreats
        val prompt = ThreatPromptBuilder.build(signals)
        return withContext(Dispatchers.IO) {
            try {
                val response = model.generateContent(prompt)
                val text = response.text
                if (text.isNullOrBlank()) {
                    Log.w(TAG, "Gemini returned empty response")
                    GeminiAnalysisResult.Unavailable
                } else {
                    GeminiAnalysisResult.Success(text.trim())
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "Gemini analysis cancelled")
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Gemini analysis failed", e)
                GeminiAnalysisResult.Unavailable
            }
        }
    }

    companion object {
        private const val TAG = "GeminiThreatAnalyzer"

        /**
         * Stable Gemini model. Do NOT downgrade to gemini-2.0-flash (EOL June 2026)
         * or gemini-1.5-pro (retired). Update here if this model is deprecated.
         */
        const val MODEL_NAME = "gemini-2.5-flash"
    }
}
