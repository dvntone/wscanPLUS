package com.wscanplus.app.cti

import com.wscanplus.core.db.dao.CtiCacheDao
import com.wscanplus.core.db.entity.CtiCacheEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CtiCacheRepositoryTest {
    // ── cache hit ────────────────────────────────────────────────────────────

    @Test
    fun `returns CacheHit when valid entry exists`() =
        runTest {
            val dao = FakeDao()
            val key = "1.2.3.4:smoke"
            dao.store[key] = entity("1.2.3.4", """{"signal":true}""", cachedAt = 0L)

            val repo = makeRepo(dao = dao, clock = { CtiCacheRepository.TTL_MS - 1 })
            val result = repo.lookup("1.2.3.4")

            assertTrue(result is CtiLookupResult.CacheHit)
            assertEquals("""{"signal":true}""", (result as CtiLookupResult.CacheHit).rawJson)
        }

    @Test
    fun `does not call network on cache hit`() =
        runTest {
            val dao = FakeDao()
            val key = "1.2.3.4:smoke"
            dao.store[key] = entity("1.2.3.4", "{}", cachedAt = 0L)
            val client = FakeClient(result = CrowdSecSmokeResult("1.2.3.4", "SHOULD_NOT_BE_CALLED"))

            val repo = makeRepo(dao = dao, client = client, clock = { CtiCacheRepository.TTL_MS - 1 })
            repo.lookup("1.2.3.4")

            assertEquals(0, client.callCount)
        }

    // ── fresh network result ─────────────────────────────────────────────────

    @Test
    fun `returns Fresh on cache miss with quota available`() =
        runTest {
            val dao = FakeDao()
            val client = FakeClient(result = CrowdSecSmokeResult("2.2.2.2", """{"ok":1}"""))
            val repo = makeRepo(dao = dao, client = client)

            val result = repo.lookup("2.2.2.2")

            assertTrue(result is CtiLookupResult.Fresh)
            assertEquals("""{"ok":1}""", (result as CtiLookupResult.Fresh).rawJson)
        }

    @Test
    fun `stores result in cache on Fresh`() =
        runTest {
            val dao = FakeDao()
            val client = FakeClient(result = CrowdSecSmokeResult("2.2.2.2", """{"ok":1}"""))
            val repo = makeRepo(dao = dao, client = client, clock = { 9999L })

            repo.lookup("2.2.2.2")

            val stored = dao.store["2.2.2.2:smoke"]
            assertEquals("""{"ok":1}""", stored?.responseJson)
            assertEquals(9999L, stored?.cachedAt)
        }

    @Test
    fun `records quota on Fresh`() =
        runTest {
            val quota = FakeQuota()
            val client = FakeClient(result = CrowdSecSmokeResult("3.3.3.3", "{}"))
            val repo = makeRepo(client = client, quota = quota)

            repo.lookup("3.3.3.3")

            assertEquals(1, quota.recorded)
        }

    @Test
    fun `does not record quota on cache hit`() =
        runTest {
            val dao = FakeDao()
            val key = "1.2.3.4:smoke"
            dao.store[key] = entity("1.2.3.4", "{}", cachedAt = 0L)
            val quota = FakeQuota()
            val repo = makeRepo(dao = dao, quota = quota, clock = { CtiCacheRepository.TTL_MS - 1 })

            repo.lookup("1.2.3.4")

            assertEquals(0, quota.recorded)
        }

    // ── quota exhausted ──────────────────────────────────────────────────────

    @Test
    fun `returns Degraded with stale when quota exhausted and stale entry exists`() =
        runTest {
            val dao = FakeDao()
            val key = "4.4.4.4:smoke"
            dao.store[key] = entity("4.4.4.4", """{"stale":true}""", cachedAt = 0L)
            val quota = FakeQuota(can = false)
            val repo = makeRepo(dao = dao, quota = quota, clock = { CtiCacheRepository.TTL_MS + 1 })

            val result = repo.lookup("4.4.4.4")

            assertTrue(result is CtiLookupResult.Degraded)
            assertEquals("""{"stale":true}""", (result as CtiLookupResult.Degraded).rawJson)
        }

    @Test
    fun `returns Unavailable when quota exhausted and no cache`() =
        runTest {
            val quota = FakeQuota(can = false)
            val repo = makeRepo(quota = quota)

            val result = repo.lookup("5.5.5.5")

            assertTrue(result is CtiLookupResult.Unavailable)
        }

    // ── network failure ──────────────────────────────────────────────────────

    @Test
    fun `returns Degraded with stale when network fails and stale entry exists`() =
        runTest {
            val dao = FakeDao()
            val key = "6.6.6.6:smoke"
            dao.store[key] = entity("6.6.6.6", """{"stale":1}""", cachedAt = 0L)
            val client = FakeClient(result = null)
            val repo = makeRepo(dao = dao, client = client, clock = { CtiCacheRepository.TTL_MS + 1 })

            val result = repo.lookup("6.6.6.6")

            assertTrue(result is CtiLookupResult.Degraded)
        }

    @Test
    fun `returns Unavailable when network fails and no cache`() =
        runTest {
            val client = FakeClient(result = null)
            val repo = makeRepo(client = client)

            val result = repo.lookup("7.7.7.7")

            assertTrue(result is CtiLookupResult.Unavailable)
        }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun makeRepo(
        dao: CtiCacheDao = FakeDao(),
        client: CtiClient = FakeClient(result = null),
        quota: CtiQuotaTracker = FakeQuota(can = true),
        clock: () -> Long = { 0L },
    ) = CtiCacheRepository(dao = dao, client = client, quotaTracker = quota, clock = clock)

    private fun entity(
        ip: String,
        json: String,
        cachedAt: Long,
    ) = CtiCacheEntity(
        cacheKey = "$ip:smoke",
        ip = ip,
        dataset = "smoke",
        responseJson = json,
        cachedAt = cachedAt,
    )

    // ── fakes ────────────────────────────────────────────────────────────────

    private class FakeDao : CtiCacheDao {
        val store = mutableMapOf<String, CtiCacheEntity>()

        override fun upsert(entry: CtiCacheEntity) {
            store[entry.cacheKey] = entry
        }

        override fun getByCacheKey(cacheKey: String): CtiCacheEntity? = store[cacheKey]

        override fun getValidEntry(
            cacheKey: String,
            ttlMs: Long,
            now: Long,
        ): CtiCacheEntity? {
            val entry = store[cacheKey] ?: return null
            return if (entry.cachedAt + ttlMs > now) entry else null
        }

        override fun evictExpired(
            ttlMs: Long,
            now: Long,
        ) {
            store.entries.removeIf { it.value.cachedAt + ttlMs <= now }
        }
    }

    private class FakeClient(
        private val result: CrowdSecSmokeResult?,
    ) : CtiClient {
        var callCount = 0

        override suspend fun lookupSmoke(ip: String): CrowdSecSmokeResult? {
            callCount++
            return result
        }
    }

    private class FakeQuota(
        private val can: Boolean = true,
    ) : CtiQuotaTracker {
        var recorded = 0

        override fun canMakeRequest(): Boolean = can

        override fun recordRequest() {
            recorded++
        }
    }
}
