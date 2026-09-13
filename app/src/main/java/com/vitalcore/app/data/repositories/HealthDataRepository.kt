package com.vitalcore.app.data.repositories

import com.vitalcore.app.data.database.VitalCoreDatabase
import com.vitalcore.app.data.database.entities.*
import com.vitalcore.app.data.healthconnect.HealthConnectManager
import com.vitalcore.app.domain.model.*
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single source of truth for health data from the domain layer's point of
 * view. Owns two responsibilities:
 *  1. sync(): pull a time range from Health Connect and persist it to Room
 *     (normalization + de-dup happens here, via IGNORE-conflict inserts).
 *  2. snapshotForDay()/snapshotsForRange(): read back from Room as domain
 *     models the calculation engines can consume.
 *
 * Calculation engines and ViewModels never call HealthConnectManager or Room
 * directly — everything goes through this repository.
 */
@Singleton
class HealthDataRepository @Inject constructor(
    private val healthConnect: HealthConnectManager,
    private val db: VitalCoreDatabase,
) {
    suspend fun sync(start: Instant, end: Instant) {
        db.heartRateDao().insertAll(
            healthConnect.readHeartRate(start, end).map { HeartRateSampleEntity(it.time.toEpochMilli(), it.bpm) }
        )
        db.hrvDao().insertAll(
            healthConnect.readHrv(start, end).map { HrvSampleEntity(it.time.toEpochMilli(), it.rmssdMillis) }
        )
        db.restingHeartRateDao().insertAll(
            healthConnect.readRestingHeartRate(start, end).map { RestingHeartRateEntity(it.date.toEpochMilli(), it.bpm) }
        )
        val sleepSessions = healthConnect.readSleep(start, end)
        db.sleepDao().insertSessions(sleepSessions.map { SleepSessionEntity(it.start.toEpochMilli(), it.end.toEpochMilli()) })
        sleepSessions.forEach { session ->
            db.sleepDao().insertStages(
                session.stages.map {
                    SleepStageEntity(
                        startEpochMillis = it.start.toEpochMilli(),
                        endEpochMillis = it.end.toEpochMilli(),
                        sessionStartEpochMillis = session.start.toEpochMilli(),
                        stage = it.stage.name,
                    )
                }
            )
        }
        db.exerciseDao().insertAll(
            healthConnect.readExerciseSessions(start, end).map {
                ExerciseSessionEntity(
                    startEpochMillis = it.start.toEpochMilli(),
                    endEpochMillis = it.end.toEpochMilli(),
                    activityType = it.activityType,
                    avgHeartRate = it.avgHeartRate,
                    maxHeartRate = it.maxHeartRate,
                    calories = it.calories,
                    distanceMeters = it.distanceMeters,
                )
            }
        )
        val activity = healthConnect.readDailyActivity(start, end)
        db.dailyActivityDao().upsert(
            DailyActivityEntity(
                dateEpochDay = LocalDate.ofInstant(start, ZoneOffset.UTC).toEpochDay(),
                steps = activity.steps,
                activeCalories = activity.activeCalories,
                totalCalories = activity.totalCalories,
                distanceMeters = activity.distanceMeters,
            )
        )
        db.vitalsDao().insertSpo2(healthConnect.readSpo2(start, end).map { SpO2SampleEntity(it.time.toEpochMilli(), it.percentage) })
        db.vitalsDao().insertRespiratory(healthConnect.readRespiratoryRate(start, end).map { RespiratoryRateEntity(it.time.toEpochMilli(), it.breathsPerMinute) })
        db.vitalsDao().insertTemperature(healthConnect.readSkinTemperature(start, end).map { TemperatureSampleEntity(it.time.toEpochMilli(), it.deltaCelsius) })
        db.vitalsDao().insertBloodPressure(healthConnect.readBloodPressure(start, end).map { BloodPressureEntity(it.time.toEpochMilli(), it.systolic, it.diastolic) })
    }

    /** Builds the normalized snapshot the calculation engines consume for one calendar day (UTC). */
    suspend fun snapshotForDay(day: LocalDate): DailyHealthSnapshot {
        val start = day.atStartOfDay(ZoneOffset.UTC).toInstant()
        val end = day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()

        val hrv = db.hrvDao().between(start.toEpochMilli(), end.toEpochMilli())
        val rhr = db.restingHeartRateDao().between(start.toEpochMilli(), end.toEpochMilli())
        val sleepEntity = db.sleepDao().sessionsBetween(start.toEpochMilli(), end.toEpochMilli()).maxByOrNull {
            it.endEpochMillis - it.startEpochMillis
        }
        val sleep = sleepEntity?.let { s ->
            val stages = db.sleepDao().stagesFor(s.startEpochMillis).map {
                SleepStageSegment(
                    start = Instant.ofEpochMilli(it.startEpochMillis),
                    end = Instant.ofEpochMilli(it.endEpochMillis),
                    stage = runCatching { SleepStageType.valueOf(it.stage) }.getOrDefault(SleepStageType.UNKNOWN),
                )
            }
            SleepSession(Instant.ofEpochMilli(s.startEpochMillis), Instant.ofEpochMilli(s.endEpochMillis), stages)
        }
        val respiratory = db.vitalsDao().respiratoryBetween(start.toEpochMilli(), end.toEpochMilli())
        val spo2 = db.vitalsDao().spo2Between(start.toEpochMilli(), end.toEpochMilli())
        val temperature = db.vitalsDao().temperatureBetween(start.toEpochMilli(), end.toEpochMilli())
        val activityEntity = db.dailyActivityDao().forDay(day.toEpochDay())
        val exercises = db.exerciseDao().between(start.toEpochMilli(), end.toEpochMilli()).map {
            ExerciseSession(
                start = Instant.ofEpochMilli(it.startEpochMillis),
                end = Instant.ofEpochMilli(it.endEpochMillis),
                activityType = it.activityType,
                avgHeartRate = it.avgHeartRate,
                maxHeartRate = it.maxHeartRate,
                calories = it.calories,
                distanceMeters = it.distanceMeters,
            )
        }

        return DailyHealthSnapshot(
            date = start,
            avgHrvMillis = hrv.map { it.rmssdMillis }.average().takeIf { !it.isNaN() },
            restingHeartRateBpm = rhr.map { it.bpm }.average().takeIf { !it.isNaN() }?.toInt(),
            sleep = sleep,
            respiratoryRateBpm = respiratory.map { it.breathsPerMinute }.average().takeIf { !it.isNaN() },
            avgSpo2Percentage = spo2.map { it.percentage }.average().takeIf { !it.isNaN() },
            skinTempDeltaCelsius = temperature.map { it.deltaCelsius }.average().takeIf { !it.isNaN() },
            activity = activityEntity?.let {
                DailyActivity(start, it.steps, it.activeCalories, it.totalCalories, it.distanceMeters)
            },
            exerciseSessions = exercises,
        )
    }

    suspend fun snapshotsForRange(startDay: LocalDate, endDayInclusive: LocalDate): List<DailyHealthSnapshot> {
        val days = generateSequence(startDay) { if (it.isBefore(endDayInclusive)) it.plusDays(1) else null }
        return days.map { snapshotForDay(it) }.toList()
    }
}
