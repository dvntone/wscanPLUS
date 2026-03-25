package com.wscanplus.core.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scan_results",
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
        Index(value = ["timestamp"]),
    ],
)
data class ScanResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val bssid: String,
    val ssid: String,
    val capabilities: String,
    val rssiDbm: Int,
    val frequencyMhz: Int,
    val channelWidth: Int,
    val timestamp: Long,
    val isHidden: Boolean,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyMeters: Float? = null,
    val altitudeMeters: Double? = null,
    val speedKph: Float? = null,
    val locationTimestamp: Long? = null,
    val locationProvider: String? = null,
    val isMockLocation: Boolean? = null,
)
