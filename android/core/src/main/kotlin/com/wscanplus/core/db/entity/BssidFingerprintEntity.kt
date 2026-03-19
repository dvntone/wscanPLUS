package com.wscanplus.core.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bssid_fingerprints")
data class BssidFingerprintEntity(
    @PrimaryKey
    val bssid: String,
    val firstSeenAt: Long,
    val lastSeenAt: Long,
    val ssids: Set<String>,
    val capabilitiesHistory: List<String>,
    val observationCount: Int,
    val ouiVendor: String?,
)
