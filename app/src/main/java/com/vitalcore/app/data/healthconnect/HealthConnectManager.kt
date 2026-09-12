package com.vitalcore.app.data.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.vitalcore.app.domain.model.BloodPressureSample
import com.vitalcore.app.domain.model.DailyActivity
import com.vitalcore.app.domain.model.ExerciseSession
import com.vitalcore.app.domain.model.HeartRateSample
import com.vitalcore.app.domain.model.HrvSample
import com.vitalcore.app.domain.model.RespiratoryRateSample
import com.vitalcore.app.domain.model.RestingHeartRateSample
import com.vitalcore.app.domain.model.SkinTemperatureSample
import com.vitalcore.app.domain.model.SleepSession
import com.vitalcore.app.domain.model.SleepStageSegment
import com.vitalcore.app.domain.model.SleepStageType
import com.vitalcore.app.domain.model.SpO2Sample
import com.vitalcore.app.domain.model.WeightSample
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single point of contact with Health Connect. Nothing else in the app
 * touches androidx.health.connect directly — this is what keeps the app
 * multi-wearable: it doesn't matter whether the underlying data came from a
 * Xiaomi band, a Garmin, or Pixel Watch, Health Connect is the only source.
 *
 * Every read method returns an empty list rather than throwing when a record
 * type isn't available/granted, so callers (repositories) can treat "no data"
 * as a normal, expected state instead of an error.
 */
@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val client: HealthConnectClient? by lazy {
        if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
        } else null
    }

    fun availability(): HealthConnectAvailability = when (HealthConnectClient.getSdkStatus(context)) {
        HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.Available
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.UpdateRequired
        else -> HealthConnectAvailability.NotInstalled
    }

    suspend fun grantedPermissions(): Set<String> {
        val c = client ?: return emptySet()
        return c.permissionController.getGrantedPermissions()
    }

    suspend fun hasAllPermissions(): Boolean = grantedPermissions().containsAll(HealthConnectPermissions.ALL)

    private fun range(start: Instant, end: Instant) = TimeRangeFilter.between(start, end)

    suspend fun readHeartRate(start: Instant, end: Instant): List<HeartRateSample> = safeRead {
        client!!.readRecords(ReadRecordsRequest(HeartRateRecord::class, range(start, end)))
            .records.flatMap { record ->
                record.samples.map { HeartRateSample(time = it.time, bpm = it.beatsPerMinute.toInt()) }
            }
    }

    suspend fun readRestingHeartRate(start: Instant, end: Instant): List<RestingHeartRateSample> = safeRead {
        client!!.readRecords(ReadRecordsRequest(RestingHeartRateRecord::class, range(start, end)))
            .records.map { RestingHeartRateSample(date = it.time, bpm = it.beatsPerMinute.toInt()) }
    }

    suspend fun readHrv(start: Instant, end: Instant): List<HrvSample> = safeRead {
        client!!.readRecords(ReadRecordsRequest(HeartRateVariabilityRmssdRecord::class, range(start, end)))
            .records.map { HrvSample(time = it.time, rmssdMillis = it.heartRateVariabilityMillis) }
    }

    suspend fun readSleep(start: Instant, end: Instant): List<SleepSession> = safeRead {
        client!!.readRecords(ReadRecordsRequest(SleepSessionRecord::class, range(start, end)))
            .records.map { record ->
                SleepSession(
                    start = record.startTime,
                    end = record.endTime,
                    stages = record.stages.map { stage ->
                        SleepStageSegment(
                            start = stage.startTime,
                            end = stage.endTime,
                            stage = mapSleepStage(stage.stage),
                        )
                    },
                )
            }
    }

    suspend fun readDailyActivity(start: Instant, end: Instant): DailyActivity = safeRead {
        val steps = client!!.readRecords(ReadRecordsRequest(StepsRecord::class, range(start, end)))
            .records.sumOf { it.count }
        val calories = client!!.readRecords(ReadRecordsRequest(TotalCaloriesBurnedRecord::class, range(start, end)))
            .records.sumOf { it.energy.inKilocalories }
        DailyActivity(
            date = start,
            steps = steps.takeIf { it > 0 },
            activeCalories = null,
            totalCalories = calories.takeIf { it > 0.0 },
            distanceMeters = null,
        )
    } ?: DailyActivity(start, null, null, null, null)

    suspend fun readExerciseSessions(start: Instant, end: Instant): List<ExerciseSession> = safeRead {
        client!!.readRecords(ReadRecordsRequest(ExerciseSessionRecord::class, range(start, end)))
            .records.map { record ->
                val hrSamples = readHeartRate(record.startTime, record.endTime)
                ExerciseSession(
                    start = record.startTime,
                    end = record.endTime,
                    activityType = record.exerciseType.toString(),
                    avgHeartRate = hrSamples.map { it.bpm }.average().takeIf { !it.isNaN() }?.toInt(),
                    maxHeartRate = hrSamples.maxOfOrNull { it.bpm },
                    calories = null,
                    distanceMeters = null,
                )
            }
    }

    suspend fun readSpo2(start: Instant, end: Instant): List<SpO2Sample> = safeRead {
        client!!.readRecords(ReadRecordsRequest(OxygenSaturationRecord::class, range(start, end)))
            .records.map { SpO2Sample(time = it.time, percentage = it.percentage.value) }
    }

    suspend fun readRespiratoryRate(start: Instant, end: Instant): List<RespiratoryRateSample> = safeRead {
        client!!.readRecords(ReadRecordsRequest(RespiratoryRateRecord::class, range(start, end)))
            .records.map { RespiratoryRateSample(time = it.time, breathsPerMinute = it.rate) }
    }

    suspend fun readSkinTemperature(start: Instant, end: Instant): List<SkinTemperatureSample> = safeRead {
        client!!.readRecords(ReadRecordsRequest(BodyTemperatureRecord::class, range(start, end)))
            .records.map { SkinTemperatureSample(time = it.time, deltaCelsius = it.temperature.inCelsius) }
    }

    suspend fun readWeight(start: Instant, end: Instant): List<WeightSample> = safeRead {
        client!!.readRecords(ReadRecordsRequest(WeightRecord::class, range(start, end)))
            .records.map { WeightSample(time = it.time, kilograms = it.weight.inKilograms) }
    }

    suspend fun readBloodPressure(start: Instant, end: Instant): List<BloodPressureSample> = safeRead {
        client!!.readRecords(ReadRecordsRequest(BloodPressureRecord::class, range(start, end)))
            .records.map {
                BloodPressureSample(
                    time = it.time,
                    systolic = it.systolic.inMillimetersOfMercury.toInt(),
                    diastolic = it.diastolic.inMillimetersOfMercury.toInt(),
                )
            }
    }

    private fun mapSleepStage(stage: Int): SleepStageType = when (stage) {
        SleepSessionRecord.STAGE_TYPE_AWAKE,
        SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED -> SleepStageType.AWAKE
        SleepSessionRecord.STAGE_TYPE_LIGHT -> SleepStageType.LIGHT
        SleepSessionRecord.STAGE_TYPE_DEEP -> SleepStageType.DEEP
        SleepSessionRecord.STAGE_TYPE_REM -> SleepStageType.REM
        else -> SleepStageType.UNKNOWN
    }

    /**
     * Every public read function is wrapped through here: if Health Connect
     * isn't available, a permission wasn't granted, or the underlying call
     * throws, we return an empty/null result instead of crashing. Missing
     * data is a normal, expected state in this app (see ALGORITHMS.md).
     */
    private suspend fun <T> safeRead(block: suspend () -> List<T>): List<T> {
        if (client == null) return emptyList()
        return try {
            withContext(Dispatchers.IO) { block() }
        } catch (t: Throwable) {
            emptyList()
        }
    }

    private suspend fun <T> safeRead(default: T? = null, block: suspend () -> T): T? {
        if (client == null) return default
        return try {
            withContext(Dispatchers.IO) { block() }
        } catch (t: Throwable) {
            default
        }
    }
}
