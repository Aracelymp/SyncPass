package com.rojas.syncpass.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserEntity::class,
        ProjectEntity::class,
        EventEntity::class,
        EnrollmentEntity::class,
        AttendanceEntity::class,
        SurveyEntity::class,
        PendingOperationEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun projectDao(): ProjectDao
    abstract fun eventDao(): EventDao
    abstract fun enrollmentDao(): EnrollmentDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun surveyDao(): SurveyDao
    abstract fun pendingOperationDao(): PendingOperationDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        /**
         * Additive migration only. Existing attendance and survey rows are preserved.
         * New nullable columns keep old records valid; redacted defaults to false.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `attendance` ADD COLUMN `registeredByUserId` INTEGER")
                db.execSQL("ALTER TABLE `surveys` ADD COLUMN `attendanceClientId` TEXT")
                db.execSQL("ALTER TABLE `surveys` ADD COLUMN `attendanceServerId` INTEGER")
                db.execSQL("ALTER TABLE `surveys` ADD COLUMN `collectedByUserId` INTEGER")
                db.execSQL("ALTER TABLE `surveys` ADD COLUMN `redacted` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `pending_operations` ADD COLUMN `attendanceClientId` TEXT")
                db.execSQL("ALTER TABLE `pending_operations` ADD COLUMN `attendanceServerId` INTEGER")

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_attendance_registeredByUserId` ON `attendance` (`registeredByUserId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_surveys_attendanceClientId` ON `surveys` (`attendanceClientId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_surveys_collectedByUserId` ON `surveys` (`collectedByUserId`)")
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "syncpass.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration(false)
                    .build()
                    .also { instance = it }
            }
    }
}
