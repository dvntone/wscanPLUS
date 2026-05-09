package com.wscanplus.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.wscanplus.core.db.entity.ScanResultEntity

@Dao
interface ScanResultDao {
    @Insert
    fun insertAll(results: List<ScanResultEntity>)

    @Query("SELECT * FROM scan_results WHERE sessionId = :sessionId")
    fun getBySession(sessionId: Long): List<ScanResultEntity>

    @Query("SELECT * FROM scan_results WHERE bssid = :bssid ORDER BY timestamp DESC LIMIT :limit")
    fun getByBssid(
        bssid: String,
        limit: Int = 100,
    ): List<ScanResultEntity>

    @Query(
        """
        SELECT * FROM scan_results
        WHERE latitude IS NOT NULL AND longitude IS NOT NULL
        ORDER BY timestamp DESC
        LIMIT :limit
        """,
    )
    fun getGpsTagged(limit: Int = 500): List<ScanResultEntity>
}
