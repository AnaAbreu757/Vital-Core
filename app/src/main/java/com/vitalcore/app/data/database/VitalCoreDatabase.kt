package com.vitalcore.app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vitalcore.app.data.database.entities.*

@Database(
    entities = [
        HeartRateSampleEntity::class,
        HrvSampleEntity::class,
        RestingHeartRateEntity::class,
        SleepSessionEntity::class,
        SleepStageEntity::class,
        ExerciseSessionEntity::class,
        DailyActivityEntity::class,
        SpO2SampleEntity::class,
        RespiratoryRateEntity::class,
        TemperatureSampleEntity::class,
        WeightSampleEntity::class,
        BloodPressureEntity::class,
        DailyMetricsEntity::class,
        RecoveryScoreEntity::class,
        SleepScoreEntity::class,
        StrainScoreEntity::class,
        InsightEntity::class,
        UserBaselineEntity::class,
        FoodEntryEntity::class,
        WaterEntryEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class VitalCoreDatabase : RoomDatabase() {
    abstract fun heartRateDao(): HeartRateDao
    abstract fun hrvDao(): HrvDao
    abstract fun restingHeartRateDao(): RestingHeartRateDao
    abstract fun sleepDao(): SleepDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun dailyActivityDao(): DailyActivityDao
    abstract fun vitalsDao(): VitalsDao
    abstract fun scoresDao(): ScoresDao
    abstract fun insightDao(): InsightDao
    abstract fun baselineDao(): BaselineDao
    abstract fun nutritionDao(): NutritionDao

    companion object {
        const val DATABASE_NAME = "vitalcore.db"

        /**
         * v1 -> v2: adds food_entries and water_entries (nutrition tracking).
         * No existing tables are touched, so this is a pure additive migration —
         * the first real Migration in this project, replacing the "no
         * migrations yet" gap noted in MANUAL.md.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `food_entries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `epochMillis` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `calories` INTEGER NOT NULL,
                        `proteinGrams` REAL,
                        `carbsGrams` REAL,
                        `fatGrams` REAL,
                        `mealType` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `water_entries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `epochMillis` INTEGER NOT NULL,
                        `milliliters` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
