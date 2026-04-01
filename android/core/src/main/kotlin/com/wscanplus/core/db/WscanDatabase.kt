package com.wscanplus.core.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
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
    version = 3,
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

        /**
         * Returns the singleton instance, constructing it on first call.
         *
         * [factory] should be a SQLCipher [SupportFactory] for at-rest encryption.
         * It is only used during the initial construction — subsequent calls return
         * the cached instance regardless of [factory].
         * Pass null only in tests (in-memory DB via Room.inMemoryDatabaseBuilder).
         */
        fun getInstance(
            context: Context,
            factory: SupportSQLiteOpenHelper.Factory? = null,
        ): WscanDatabase =
            instance
                ?: synchronized(this) {
                    instance
                        ?: buildDatabase(context, factory).also {
                            instance = it
                        }
                }

        private fun buildDatabase(
            context: Context,
            factory: SupportSQLiteOpenHelper.Factory?,
        ): WscanDatabase {
            val builder =
                Room
                    .databaseBuilder(
                        context.applicationContext,
                        WscanDatabase::class.java,
                        "wscan.db",
                    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            factory?.let { builder.openHelperFactory(it) }
            return builder.build()
        }

        private val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE scan_results ADD COLUMN latitude REAL")
                    db.execSQL("ALTER TABLE scan_results ADD COLUMN longitude REAL")
                    db.execSQL("ALTER TABLE scan_results ADD COLUMN accuracyMeters REAL")
                    db.execSQL("ALTER TABLE scan_results ADD COLUMN altitudeMeters REAL")
                    db.execSQL("ALTER TABLE scan_results ADD COLUMN speedKph REAL")
                    db.execSQL("ALTER TABLE scan_results ADD COLUMN locationTimestamp INTEGER")
                    db.execSQL("ALTER TABLE scan_results ADD COLUMN locationProvider TEXT")
                    db.execSQL("ALTER TABLE scan_results ADD COLUMN isMockLocation INTEGER")
                }
            }

        private val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_scan_results_timestamp ON scan_results (timestamp)")
                }
            }
    }
}
