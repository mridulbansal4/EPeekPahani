package io.sc.eppCordova.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.sc.eppCordova.data.local.dao.AdminUnitDao
import io.sc.eppCordova.data.local.dao.CropRecordDao
import io.sc.eppCordova.data.local.dao.LandRecordDao
import io.sc.eppCordova.data.local.dao.LossClaimDao
import io.sc.eppCordova.data.local.dao.SyncQueueDao
import io.sc.eppCordova.data.local.entity.AdminUnit
import io.sc.eppCordova.data.local.entity.CropRecord
import io.sc.eppCordova.data.local.entity.Farmer
import io.sc.eppCordova.data.local.entity.LandRecord
import io.sc.eppCordova.data.local.entity.LossClaimEntity
import io.sc.eppCordova.data.local.entity.SyncQueueEntity

@Database(
    entities = [Farmer::class, AdminUnit::class, LandRecord::class, CropRecord::class, SyncQueueEntity::class, LossClaimEntity::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cropRecordDao(): CropRecordDao
    abstract fun adminUnitDao(): AdminUnitDao
    abstract fun landRecordDao(): LandRecordDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun lossClaimDao(): LossClaimDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns to crop_records
                database.execSQL("ALTER TABLE crop_records ADD COLUMN aiMatchStatus TEXT NOT NULL DEFAULT 'PENDING'")
                database.execSQL("ALTER TABLE crop_records ADD COLUMN aiDetectedCrop TEXT")
                database.execSQL("ALTER TABLE crop_records ADD COLUMN aiConfidence REAL")
                database.execSQL("ALTER TABLE crop_records ADD COLUMN photo3Uri TEXT")
                database.execSQL("ALTER TABLE crop_records ADD COLUMN certificateId TEXT")

                // Add boundary polygon to land_records
                database.execSQL("ALTER TABLE land_records ADD COLUMN boundaryPolygonJson TEXT")

                // Create sync_queue table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sync_queue` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `itemType` TEXT NOT NULL, 
                        `itemId` TEXT NOT NULL, 
                        `priority` INTEGER NOT NULL, 
                        `retryCount` INTEGER NOT NULL, 
                        `lastAttemptAt` INTEGER NOT NULL, 
                        `status` TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }
        
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `loss_claims` (
                        `claimId` TEXT NOT NULL, 
                        `farmerId` TEXT NOT NULL, 
                        `registrationId` TEXT NOT NULL, 
                        `gatNumber` TEXT NOT NULL, 
                        `lossType` TEXT NOT NULL, 
                        `incidentDate` TEXT NOT NULL, 
                        `reportedAffectedAreaHa` REAL NOT NULL, 
                        `surveyMode` TEXT NOT NULL, 
                        `geoFenceStatus` TEXT NOT NULL, 
                        `videoClipsJson` TEXT NOT NULL, 
                        `aiFramesJson` TEXT NOT NULL, 
                        `gpsTrailJson` TEXT NOT NULL, 
                        `weatherCorrelationScore` INTEGER, 
                        `ndviDrop` REAL, 
                        `damageSeverity` TEXT, 
                        `fraudRiskScore` INTEGER, 
                        `recommendedCompensationPct` INTEGER, 
                        `status` TEXT NOT NULL, 
                        `isSubmitted` INTEGER NOT NULL, 
                        `createdAt` INTEGER NOT NULL, 
                        PRIMARY KEY(`claimId`)
                    )
                """.trimIndent())
            }
        }
    }
}
