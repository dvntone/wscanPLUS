package com.wscanplus.app.cti

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject

object CrowdSecSmokeParser {
    data class ParsedScore(
        val aggressiveScore: Int?,
        val backgroundNoiseScore: Int?,
    ) {
        val hasSignal: Boolean
            get() = (aggressiveScore ?: 0) > 0 || (backgroundNoiseScore ?: 0) > 0

        val confidence: Float?
            get() {
                val max = maxOf(aggressiveScore ?: 0, backgroundNoiseScore ?: 0)
                if (max <= 0) return null
                return (max.toFloat() / 100f).coerceIn(0f, 0.9f)
            }

        fun reasons(): List<String> {
            val out = mutableListOf<String>()
            aggressiveScore?.let { if (it > 0) out.add("CrowdSec aggressive score: $it/100") }
            backgroundNoiseScore?.let { if (it > 0) out.add("CrowdSec background noise score: $it/100") }
            return out.take(3)
        }
    }

    fun hasSignal(rawJson: String): Boolean = rawJson.isNotBlank()

    fun parse(rawJson: String): ParsedScore {
        if (rawJson.isBlank()) return ParsedScore(null, null)
        return try {
            val obj = Json.parseToJsonElement(rawJson).jsonObject
            ParsedScore(
                aggressiveScore = (obj["aggressive_score"] as? JsonPrimitive)?.intOrNull,
                backgroundNoiseScore = (obj["background_noise_score"] as? JsonPrimitive)?.intOrNull,
            )
        } catch (_: SerializationException) {
            ParsedScore(null, null)
        }
    }
}
