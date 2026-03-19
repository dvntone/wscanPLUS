package com.wscanplus.core.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.wscanplus.core.threat.EnvironmentType

@Entity(tableName = "scan_sessions")
data class ScanSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startedAt: Long,
    val endedAt: Long?,
    val environmentType: EnvironmentType,
    val deviceSerial: String,
)
