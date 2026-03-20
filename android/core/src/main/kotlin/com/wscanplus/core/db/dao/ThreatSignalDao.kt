package com.wscanplus.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.wscanplus.core.db.entity.ThreatSignalEntity

@Dao
interface ThreatSignalDao {
    @Insert
    fun insert(signal: ThreatSignalEntity): Long

    @Insert
    fun insertAll(signals: List<ThreatSignalEntity>)

    @Query("SELECT * FROM threat_signals WHERE sessionId = :sessionId")
    fun getBySession(sessionId: Long): List<ThreatSignalEntity>

    @Query("SELECT * FROM threat_signals WHERE bssid = :bssid ORDER BY detectedAt DESC LIMIT :limit")
    fun getByBssid(
        bssid: String,
        limit: Int = 50,
    ): List<ThreatSignalEntity>

    @Query("SELECT * FROM threat_signals WHERE confidence >= :minConfidence ORDER BY detectedAt DESC LIMIT :limit")
    fun getHighConfidence(
        minConfidence: Float,
        limit: Int = 100,
    ): List<ThreatSignalEntity>
}
