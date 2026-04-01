package com.wscanplus.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.wscanplus.core.db.entity.ScanSessionEntity

@Dao
interface ScanSessionDao {
    @Insert
    fun insert(session: ScanSessionEntity): Long

    @Query("UPDATE scan_sessions SET endedAt = :endedAt WHERE id = :sessionId")
    fun markCompleted(
        sessionId: Long,
        endedAt: Long,
    )

    @Query("SELECT * FROM scan_sessions ORDER BY startedAt DESC LIMIT :limit")
    fun getRecent(limit: Int): List<ScanSessionEntity>

    @Query(
        "SELECT * FROM scan_sessions WHERE startedAt >= :from AND startedAt <= :to ORDER BY startedAt DESC",
    )
    fun getByTimeRange(
        from: Long,
        to: Long,
    ): List<ScanSessionEntity>

    /**
     * Delete completed sessions whose [ScanSessionEntity.endedAt] is before [cutoffMs].
     * Deletion cascades to scan_results and threat_signals via their FK constraints.
     * Returns the number of sessions deleted.
     */
    @Query("DELETE FROM scan_sessions WHERE endedAt IS NOT NULL AND endedAt < :cutoffMs")
    fun deleteCompletedOlderThan(cutoffMs: Long): Int
}
