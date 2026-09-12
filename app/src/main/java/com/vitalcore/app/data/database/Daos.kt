package com.vitalcore.app.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vitalcore.app.data.database.entities.*
import kotlinx.coroutines.flow.Flow

/**
 * All inserts use IGNORE conflict strategy on the entity's primary key
 * (a timestamp or date). Re-ingesting the same Health Connect window is
 * therefore always safe and never creates duplicate rows.
 */

@Dao
interface HeartRateDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(samples: List<HeartRateSampleEntity>)

    @Query("SELECT * FROM heart_rate_samples WHERE epochMillis BETWEEN :startMillis AND :endMillis ORDER BY epochMillis")
    suspend fun between(startMillis: Long, endMillis: Long): List<HeartRateSampleEntity>
}

@Dao
interface HrvDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(samples: List<HrvSampleEntity>)

    @Query("SELECT * FROM hrv_samples WHERE epochMillis BETWEEN :startMillis AND :endMillis ORDER BY epochMillis")
    suspend fun between(startMillis: Long, endMillis: Long): List<HrvSampleEntity>
}

@Dao
interface RestingHeartRateDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(samples: List<RestingHeartRateEntity>)

    @Query("SELECT * FROM resting_heart_rate_samples WHERE epochMillis BETWEEN :startMillis AND :endMillis ORDER BY epochMillis")
    suspend fun between(startMillis: Long, endMillis: Long): List<RestingHeartRateEntity>
}

@Dao
interface SleepDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSessions(sessions: List<SleepSessionEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStages(stages: List<SleepStageEntity>)

    @Query("SELECT * FROM sleep_sessions WHERE startEpochMillis BETWEEN :startMillis AND :endMillis ORDER BY startEpochMillis")
    suspend fun sessionsBetween(startMillis: Long, endMillis: Long): List<SleepSessionEntity>

    @Query("SELECT * FROM sleep_stages WHERE sessionStartEpochMillis = :sessionStart ORDER BY startEpochMillis")
    suspend fun stagesFor(sessionStart: Long): List<SleepStageEntity>
}

@Dao
interface ExerciseDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(sessions: List<ExerciseSessionEntity>)

    @Query("SELECT * FROM exercise_sessions WHERE startEpochMillis BETWEEN :startMillis AND :endMillis ORDER BY startEpochMillis")
    suspend fun between(startMillis: Long, endMillis: Long): List<ExerciseSessionEntity>
}

@Dao
interface DailyActivityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DailyActivityEntity)

    @Query("SELECT * FROM daily_activity WHERE dateEpochDay = :epochDay")
    suspend fun forDay(epochDay: Long): DailyActivityEntity?

    @Query("SELECT * FROM daily_activity WHERE dateEpochDay BETWEEN :startDay AND :endDay ORDER BY dateEpochDay")
    fun observeRange(startDay: Long, endDay: Long): Flow<List<DailyActivityEntity>>
}

@Dao
interface VitalsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSpo2(samples: List<SpO2SampleEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRespiratory(samples: List<RespiratoryRateEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTemperature(samples: List<TemperatureSampleEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWeight(samples: List<WeightSampleEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBloodPressure(samples: List<BloodPressureEntity>)

    @Query("SELECT * FROM spo2_samples WHERE epochMillis BETWEEN :startMillis AND :endMillis")
    suspend fun spo2Between(startMillis: Long, endMillis: Long): List<SpO2SampleEntity>

    @Query("SELECT * FROM respiratory_rate_samples WHERE epochMillis BETWEEN :startMillis AND :endMillis")
    suspend fun respiratoryBetween(startMillis: Long, endMillis: Long): List<RespiratoryRateEntity>

    @Query("SELECT * FROM temperature_samples WHERE epochMillis BETWEEN :startMillis AND :endMillis")
    suspend fun temperatureBetween(startMillis: Long, endMillis: Long): List<TemperatureSampleEntity>
}

@Dao
interface ScoresDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyMetrics(entity: DailyMetricsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecovery(entity: RecoveryScoreEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSleep(entity: SleepScoreEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStrain(entity: StrainScoreEntity)

    @Query("SELECT * FROM recovery_scores WHERE dateEpochDay BETWEEN :startDay AND :endDay ORDER BY dateEpochDay")
    fun observeRecoveryRange(startDay: Long, endDay: Long): Flow<List<RecoveryScoreEntity>>

    @Query("SELECT * FROM sleep_scores WHERE dateEpochDay BETWEEN :startDay AND :endDay ORDER BY dateEpochDay")
    fun observeSleepRange(startDay: Long, endDay: Long): Flow<List<SleepScoreEntity>>

    @Query("SELECT * FROM strain_scores WHERE dateEpochDay BETWEEN :startDay AND :endDay ORDER BY dateEpochDay")
    fun observeStrainRange(startDay: Long, endDay: Long): Flow<List<StrainScoreEntity>>

    @Query("SELECT * FROM recovery_scores WHERE dateEpochDay = :epochDay")
    suspend fun recoveryForDay(epochDay: Long): RecoveryScoreEntity?

    @Query("SELECT * FROM sleep_scores WHERE dateEpochDay = :epochDay")
    suspend fun sleepForDay(epochDay: Long): SleepScoreEntity?

    @Query("SELECT * FROM strain_scores WHERE dateEpochDay = :epochDay")
    suspend fun strainForDay(epochDay: Long): StrainScoreEntity?
}

@Dao
interface InsightDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(insight: InsightEntity)

    @Query("SELECT * FROM insights WHERE dateEpochDay = :epochDay ORDER BY id DESC")
    fun observeForDay(epochDay: Long): Flow<List<InsightEntity>>

    @Query("DELETE FROM insights")
    suspend fun deleteAll()
}

@Dao
interface BaselineDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserBaselineEntity)

    @Query("SELECT * FROM user_baselines WHERE metricKey = :key")
    suspend fun get(key: String): UserBaselineEntity?

    @Query("SELECT * FROM user_baselines")
    suspend fun getAll(): List<UserBaselineEntity>

    @Query("DELETE FROM user_baselines")
    suspend fun deleteAll()
}

@Dao
interface NutritionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(entry: FoodEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWater(entry: WaterEntryEntity): Long

    @Query("DELETE FROM food_entries WHERE id = :id")
    suspend fun deleteFood(id: Long)

    @Query("DELETE FROM water_entries WHERE id = :id")
    suspend fun deleteWater(id: Long)

    @Query("SELECT * FROM food_entries WHERE epochMillis BETWEEN :startMillis AND :endMillis ORDER BY epochMillis")
    fun observeFoodBetween(startMillis: Long, endMillis: Long): Flow<List<FoodEntryEntity>>

    @Query("SELECT * FROM water_entries WHERE epochMillis BETWEEN :startMillis AND :endMillis ORDER BY epochMillis")
    fun observeWaterBetween(startMillis: Long, endMillis: Long): Flow<List<WaterEntryEntity>>
}
