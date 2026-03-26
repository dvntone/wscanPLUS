package com.wscanplus.app.cti

import android.util.Log
import com.wscanplus.core.db.dao.CtiCacheDao
import com.wscanplus.core.db.entity.CtiCacheEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Cache-first CrowdSec CTI lookup with 50 req/day quota enforcement.
 *
 * Lookup priority:
 *  1. Valid cached entry (within [TTL_MS]) → [CtiLookupResult.CacheHit], no network call.
 *  2. Quota available → live network call → [CtiLookupResult.Fresh], entry cached.
 *  3. Quota exhausted or network failure → stale cache → [CtiLookupResult.Degraded].
 *  4. No cache at all → [CtiLookupResult.Unavailable].
 *
 * All DAO and network operations run on [Dispatchers.IO].
 */
class CtiCacheRepository(
    private val dao: CtiCacheDao,
    private val client: CtiClient,
    private val quotaTracker: CtiQuotaTracker,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend fun lookup(ip: String): CtiLookupResult =
        withContext(Dispatchers.IO) {
            val key = cacheKey(ip)
            val now = clock()

            val cached = dao.getValidEntry(key, TTL_MS, now)
            if (cached != null) {
                return@withContext CtiLookupResult.CacheHit(cached.responseJson)
            }

            if (!quotaTracker.canMakeRequest()) {
                Log.w(TAG, "CrowdSec CTI daily quota exhausted — returning stale cache for $ip")
                val stale = dao.getByCacheKey(key)
                return@withContext if (stale != null) {
                    CtiLookupResult.Degraded(stale.responseJson)
                } else {
                    CtiLookupResult.Unavailable
                }
            }

            val result = client.lookupSmoke(ip)
            if (result != null) {
                quotaTracker.recordRequest()
                dao.upsert(
                    CtiCacheEntity(
                        cacheKey = key,
                        ip = ip,
                        dataset = DATASET,
                        responseJson = result.rawJson,
                        cachedAt = now,
                    ),
                )
                CtiLookupResult.Fresh(result.rawJson)
            } else {
                Log.w(TAG, "CrowdSec CTI network lookup failed for $ip — falling back to stale cache")
                val stale = dao.getByCacheKey(key)
                if (stale != null) {
                    CtiLookupResult.Degraded(stale.responseJson)
                } else {
                    CtiLookupResult.Unavailable
                }
            }
        }

    /** Evict expired entries. Call during maintenance (e.g. WatchdogService startup). */
    suspend fun pruneExpired() =
        withContext(Dispatchers.IO) {
            dao.evictExpired(TTL_MS, clock())
        }

    private fun cacheKey(ip: String): String = "$ip:$DATASET"

    companion object {
        private const val TAG = "CtiCacheRepository"
        private const val DATASET = "smoke"
        internal const val TTL_MS = 24L * 60 * 60 * 1000 // 24 hours
    }
}
