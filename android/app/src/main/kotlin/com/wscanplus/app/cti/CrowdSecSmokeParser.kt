package com.wscanplus.app.cti

import org.json.JSONException
import org.json.JSONObject

/**
 * Parses CrowdSec CTI /v2/smoke response JSON.
 *
 * Fields used:
 *  - aggressive_score  (0–100): frequency of attacks attributed to this IP. Higher = more aggressive.
 *  - background_noise_score (0–100): volume of background noise (scanning, probing). Higher = noisier.
 *
 * Both fields may be absent (null) for IPs with no CTI record — treated as no signal.
 * Confidence is derived by normalising the higher of the two scores to [0.0, 0.9].
 */
object CrowdSecSmokeParser {
    data class ParsedScore(
        val aggressiveScore: Int?,
        val backgroundNoiseScore: Int?,
    ) {
        /** True if either score is present and non-zero. */
        val hasSignal: Boolean
            get() = (aggressiveScore ?: 0) > 0 || (backgroundNoiseScore ?: 0) > 0

        /**
         * Confidence in [0.0, 0.9] derived from the higher of the two scores.
         * Returns null when [hasSignal] is false.
         */
        val confidence: Float?
            get() {
                val max = maxOf(aggressiveScore ?: 0, backgroundNoiseScore ?: 0)
                if (max <= 0) return null
                return (max.toFloat() / 100f).coerceIn(0f, 0.9f)
            }

        /** Human-readable reasons for UI / audit log (max 3). */
        fun reasons(): List<String> {
            val out = mutableListOf<String>()
            aggressiveScore?.let { if (it > 0) out.add("CrowdSec aggressive score: $it/100") }
            backgroundNoiseScore?.let { if (it > 0) out.add("CrowdSec background noise score: $it/100") }
            return out.take(3)
        }
    }

    /** Returns true if the response body contains any CTI signal worth recording. */
    fun hasSignal(rawJson: String): Boolean = rawJson.isNotBlank()

    /**
     * Parses [rawJson] into a [ParsedScore].
     * Returns [ParsedScore] with null fields if the JSON is malformed or fields are absent.
     */
    fun parse(rawJson: String): ParsedScore {
        if (rawJson.isBlank()) return ParsedScore(null, null)
        return try {
            val obj = JSONObject(rawJson)
            ParsedScore(
                aggressiveScore = obj.optInt("aggressive_score", -1).takeIf { it >= 0 },
                backgroundNoiseScore = obj.optInt("background_noise_score", -1).takeIf { it >= 0 },
            )
        } catch (_: JSONException) {
            ParsedScore(null, null)
        }
    }
}
