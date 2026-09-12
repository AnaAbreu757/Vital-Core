package com.vitalcore.app.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entities mirroring the domain models. Kept as simple, flat tables —
 * one row per sample/session/score. `sourceTimestamp` is the dedupe key used
 * by repositories (see InsertOrIgnore usage in the DAOs) so re-reading the
 * same Health Connect window twice never creates duplicate rows.
 */

@Entity(tableName = "heart_rate_samples", primaryKeys = ["epochMillis"])
data class HeartRateSampleEntity(val epochMillis: Long, val bpm: Int)

@Entity(tableName = "hrv_samples", primaryKeys = ["epochMillis"])
data class HrvSampleEntity(val epochMillis: Long, val rmssdMillis: Double)

@Entity(tableName = "resting_heart_rate_samples", primaryKeys = ["epochMillis"])
data class RestingHeartRateEntity(val epochMillis: Long, val bpm: Int)

@Entity(tableName = "sleep_sessions", primaryKeys = ["startEpochMillis"])
data class SleepSessionEntity(
    val startEpochMillis: Long,
    val endEpochMillis: Long,
)

@Entity(tableName = "sleep_stages", primaryKeys = ["startEpochMillis"])
data class SleepStageEntity(
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val sessionStartEpochMillis: Long,
    val stage: String,
)

@Entity(tableName = "exercise_sessions", primaryKeys = ["startEpochMillis"])
data class ExerciseSessionEntity(
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val activityType: String,
    val avgHeartRate: Int?,
    val maxHeartRate: Int?,
    val calories: Double?,
    val distanceMeters: Double?,
)

@Entity(tableName = "daily_activity", primaryKeys = ["dateEpochDay"])
data class DailyActivityEntity(
    val dateEpochDay: Long,
    val steps: Long?,
    val activeCalories: Double?,
    val totalCalories: Double?,
    val distanceMeters: Double?,
)

@Entity(tableName = "spo2_samples", primaryKeys = ["epochMillis"])
data class SpO2SampleEntity(val epochMillis: Long, val percentage: Double)

@Entity(tableName = "respiratory_rate_samples", primaryKeys = ["epochMillis"])
data class RespiratoryRateEntity(val epochMillis: Long, val breathsPerMinute: Double)

@Entity(tableName = "temperature_samples", primaryKeys = ["epochMillis"])
data class TemperatureSampleEntity(val epochMillis: Long, val deltaCelsius: Double)

@Entity(tableName = "weight_samples", primaryKeys = ["epochMillis"])
data class WeightSampleEntity(val epochMillis: Long, val kilograms: Double)

@Entity(tableName = "blood_pressure_samples", primaryKeys = ["epochMillis"])
data class BloodPressureEntity(val epochMillis: Long, val systolic: Int, val diastolic: Int)

/** One row per day: the inputs that fed that day's scores, for audit/debugging. */
@Entity(tableName = "daily_metrics", primaryKeys = ["dateEpochDay"])
data class DailyMetricsEntity(
    val dateEpochDay: Long,
    val avgHrvMillis: Double?,
    val restingHeartRateBpm: Int?,
    val sleepDurationMinutes: Long?,
    val respiratoryRateBpm: Double?,
    val avgSpo2Percentage: Double?,
    val skinTempDeltaCelsius: Double?,
    val dataQualityPercent: Int,
)

@Entity(tableName = "recovery_scores", primaryKeys = ["dateEpochDay"])
data class RecoveryScoreEntity(
    val dateEpochDay: Long,
    val score: Int,
    val confidence: String,
    val hrvContributionPercent: Double?,
    val sleepContributionPercent: Double?,
    val rhrContributionPercent: Double?,
)

@Entity(tableName = "sleep_scores", primaryKeys = ["dateEpochDay"])
data class SleepScoreEntity(
    val dateEpochDay: Long,
    val score: Int,
    val durationMinutes: Long,
    val efficiencyPercent: Int?,
    val sleepDebtMinutes: Long?,
)

@Entity(tableName = "strain_scores", primaryKeys = ["dateEpochDay"])
data class StrainScoreEntity(
    val dateEpochDay: Long,
    val score: Double,
    val trainingLoad: Double,
)

@Entity(tableName = "insights")
data class InsightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val text: String,
    val category: String,
)

/** One row per metric per user: the learned rolling baseline for that signal. */
@Entity(tableName = "user_baselines", primaryKeys = ["metricKey"])
data class UserBaselineEntity(
    val metricKey: String,
    val mean: Double,
    val standardDeviation: Double,
    val sampleCount: Int,
    val lastUpdatedEpochDay: Long,
)

@Entity(tableName = "food_entries")
data class FoodEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochMillis: Long,
    val name: String,
    val calories: Int,
    val proteinGrams: Double?,
    val carbsGrams: Double?,
    val fatGrams: Double?,
    val mealType: String,
)

@Entity(tableName = "water_entries")
data class WaterEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochMillis: Long,
    val milliliters: Int,
)
