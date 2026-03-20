package com.wscanplus.core.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.wscanplus.core.db.dao.BssidFingerprintDao
import com.wscanplus.core.db.dao.CtiCacheDao
import com.wscanplus.core.db.dao.ScanResultDao
import com.wscanplus.core.db.dao.ScanSessionDao
import com.wscanplus.core.db.dao.ThreatSignalDao
import com.wscanplus.core.db.entity.BssidFingerprintEntity
import com.wscanplus.core.db.entity.CtiCacheEntity
import com.wscanplus.core.db.entity.ScanResultEntity
import com.wscanplus.core.db.entity.ScanSessionEntity
import com.wscanplus.core.db.entity.ThreatSignalEntity

@Database(
    entities = [
        ScanSessionEntity::class,
        ScanResultEntity::class,
        BssidFingerprintEntity::class,
        ThreatSignalEntity::class,
        CtiCacheEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class WscanDatabase : RoomDatabase() {
    abstract fun scanSessionDao(): ScanSessionDao

    abstract fun scanResultDao(): ScanResultDao

    abstract fun bssidFingerprintDao(): BssidFingerprintDao

    abstract fun threatSignalDao(): ThreatSignalDao

    abstract fun ctiCacheDao(): CtiCacheDao

    companion object {
        @Volatile
        private var instance: WscanDatabase? = null

        fun getInstance(context: Context): WscanDatabase =
            instance
                ?: synchronized(this) {
                    instance
                        ?: buildDatabase(context).also {
                            instance = it
                        }
                }

        private fun buildDatabase(context: Context): WscanDatabase =
            Room
                .databaseBuilder(
                    context.applicationContext,
                    WscanDatabase::class.java,
                    "wscan.db",
                ).fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
