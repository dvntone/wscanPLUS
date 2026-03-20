package com.wscanplus.core.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.wscanplus.core.db.entity.CtiCacheEntity

@Dao
interface CtiCacheDao {
    @Upsert
    fun upsert(entry: CtiCacheEntity)

    @Query("SELECT * FROM cti_cache WHERE cacheKey = :cacheKey")
    fun getByCacheKey(cacheKey: String): CtiCacheEntity?

    @Query("SELECT * FROM cti_cache WHERE cacheKey = :cacheKey AND cachedAt + :ttlMs > :now")
    fun getValidEntry(
        cacheKey: String,
        ttlMs: Long,
        now: Long,
    ): CtiCacheEntity?

    @Query("DELETE FROM cti_cache WHERE cachedAt + :ttlMs <= :now")
    fun evictExpired(
        ttlMs: Long,
        now: Long,
    )
}
