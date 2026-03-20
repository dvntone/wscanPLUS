package com.wscanplus.core.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.wscanplus.core.db.entity.BssidFingerprintEntity

@Dao
interface BssidFingerprintDao {
    @Upsert
    fun upsert(fingerprint: BssidFingerprintEntity)

    @Query("SELECT * FROM bssid_fingerprints WHERE bssid = :bssid")
    fun getByBssid(bssid: String): BssidFingerprintEntity?

    @Query("SELECT * FROM bssid_fingerprints WHERE lastSeenAt >= :since ORDER BY lastSeenAt DESC")
    fun getRecentlyActive(since: Long): List<BssidFingerprintEntity>

    @Query("SELECT COUNT(*) FROM bssid_fingerprints")
    fun getKnownBssidCount(): Int
}
