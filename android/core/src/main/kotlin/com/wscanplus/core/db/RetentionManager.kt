package com.wscanplus.core.db

import android.util.Log
import com.wscanplus.core.db.dao.ScanSessionDao

/**
 * Purges completed scan sessions older than [retentionMs].
 *
 * Deletion cascades via the ON DELETE CASCADE foreign keys on scan_results
 * and threat_signals — no separate purge of those tables is required.
 *
 * [purge] is a blocking call; invoke from a background thread or IO dispatcher.
 */
class RetentionManager(
    private val sessionDao: ScanSessionDao,
    private val retentionMs: Long = DEFAULT_RETENTION_MS,
) {
    fun purge(now: Long = System.currentTimeMillis()) {
        val cutoff = now - retentionMs
        val deleted = sessionDao.deleteCompletedOlderThan(cutoff)
        Log.i(TAG, "Retention purge: $deleted session(s) older than ${retentionMs / MS_PER_DAY}d removed")
    }

    companion object {
        private const val TAG = "RetentionManager"
        private const val MS_PER_DAY = 24L * 60 * 60 * 1000
        const val DEFAULT_RETENTION_DAYS = 30L
        val DEFAULT_RETENTION_MS = DEFAULT_RETENTION_DAYS * MS_PER_DAY
    }
}
