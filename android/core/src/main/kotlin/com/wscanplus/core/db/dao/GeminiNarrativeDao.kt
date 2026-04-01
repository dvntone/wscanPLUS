package com.wscanplus.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.wscanplus.core.db.entity.GeminiNarrativeEntity

@Dao
interface GeminiNarrativeDao {
    @Insert
    fun insert(narrative: GeminiNarrativeEntity): Long

    @Query("SELECT * FROM gemini_narratives WHERE sessionId = :sessionId ORDER BY generatedAt DESC")
    fun getBySession(sessionId: Long): List<GeminiNarrativeEntity>

    @Query("SELECT * FROM gemini_narratives ORDER BY generatedAt DESC LIMIT :limit")
    fun getRecent(limit: Int = 50): List<GeminiNarrativeEntity>
}
