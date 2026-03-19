package com.wscanplus.core.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cti_cache",
    indices = [
        Index(value = ["ip", "dataset"]),
    ],
)
data class CtiCacheEntity(
    @PrimaryKey
    val cacheKey: String,
    val ip: String,
    val dataset: String,
    val responseJson: String,
    val cachedAt: Long,
)
