package com.vitalcore.app.domain.model

import java.time.Instant
import java.time.ZoneId

/**
 * Pure-Kotlin domain models. These are what flows through healthconnect ->
 * repositories -> calculations -> ui. Room entities (data/database/entities)
 * mirror these for persistence but domain code never imports a Room entity
 * directly, so calculations stay testable without Android/Room on the classpath.
 */

data class HeartRateSample(val time: Instant, val bpm: Int, val zoneId: ZoneId = ZoneId.systemDefault())

data class HrvSample(val time: Instant, val rmssdMillis: Double)

data class RestingHeartRateSample(val date: Instant, val bpm: Int)

data class SleepStageSegment(val start: Instant, val end: Instant, val stage: SleepStageType)

enum class SleepStageType { AWAKE, LIGHT, DEEP, REM, UNKNOWN }

data class SleepSession(
    val start: Instant,
    val end: Instant,
    val stages: List<SleepStageSegment> = emptyList(),
) {
    val durationMinutes: Long get() = java.time.Duration.between(start, end).toMinutes()
}

data class ExerciseSession(
    val start: Instant,
    val end: Instant,
    val activityType: String,
    val avgHeartRate: Int?,
    val maxHeartRate: Int?,
    val calories: Double?,
    val distanceMeters: Double?,
)

data class DailyActivity(
    val date: Instant,
    val steps: Long?,
    val activeCalories: Double?,
    val totalCalories: Double?,
    val distanceMeters: Double?,
)

data class SpO2Sample(val time: Instant, val percentage: Double)

data class RespiratoryRateSample(val time: Instant, val breathsPerMinute: Double)

data class SkinTemperatureSample(val time: Instant, val deltaCelsius: Double)

data class WeightSample(val time: Instant, val kilograms: Double)

data class BloodPressureSample(val time: Instant, val systolic: Int, val diastolic: Int)

/**
 * Everything the calculation engine might use for a single day, already
 * normalized. Any field can be null — the engines must handle that rather
 * than assuming data exists (per the "never invent data" principle).
 */
data class DailyHealthSnapshot(
    val date: Instant,
    val avgHrvMillis: Double?,
    val restingHeartRateBpm: Int?,
    val sleep: SleepSession?,
    val respiratoryRateBpm: Double?,
    val avgSpo2Percentage: Double?,
    val skinTempDeltaCelsius: Double?,
    val activity: DailyActivity?,
    val exerciseSessions: List<ExerciseSession> = emptyList(),
)
