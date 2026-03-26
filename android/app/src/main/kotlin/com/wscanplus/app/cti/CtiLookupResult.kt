package com.wscanplus.app.cti

/**
 * Result of a [CtiCacheRepository.lookup] call.
 *
 * - [Fresh]    — live network result, quota consumed, entry written to cache.
 * - [CacheHit] — valid cached entry returned, no network call made.
 * - [Degraded] — stale cached entry returned (quota exhausted or network failed).
 * - [Unavailable] — no cache and no network result available.
 */
sealed class CtiLookupResult {
    data class Fresh(
        val rawJson: String,
    ) : CtiLookupResult()

    data class CacheHit(
        val rawJson: String,
    ) : CtiLookupResult()

    data class Degraded(
        val rawJson: String,
    ) : CtiLookupResult()

    object Unavailable : CtiLookupResult()
}
