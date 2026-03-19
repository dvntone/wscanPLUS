package com.wscanplus.core.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.wscanplus.core.threat.HeuristicType
import com.wscanplus.core.threat.ThreatSource

@Entity(
    tableName = "threat_signals",
    foreignKeys = [
        ForeignKey(
            entity = ScanSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["bssid"]),
    ],
)
data class ThreatSignalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val bssid: String,
    val confidence: Float,
    val source: ThreatSource,
    val heuristicType: HeuristicType?,
    val reasons: List<String>,
    val detectedAt: Long,
    val schemaVersion: Int = 1,
)
