package com.vitalcore.app.data.repositories

import com.vitalcore.app.data.database.VitalCoreDatabase
import com.vitalcore.app.data.database.entities.InsightEntity
import com.vitalcore.app.data.database.entities.RecoveryScoreEntity
import com.vitalcore.app.data.database.entities.SleepScoreEntity
import com.vitalcore.app.data.database.entities.StrainScoreEntity
import com.vitalcore.app.data.database.entities.UserBaselineEntity
import com.vitalcore.app.domain.calculations.BaselineStats
import com.vitalcore.app.domain.calculations.Insight
import com.vitalcore.app.domain.calculations.RecoveryResult
import com.vitalcore.app.domain.calculations.SleepResult
import com.vitalcore.app.domain.calculations.StrainResult
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Persists and retrieves calculation-engine outputs (scores, baselines, insights). */
@Singleton
class ScoreRepository @Inject constructor(private val db: VitalCoreDatabase) {

    suspend fun saveRecovery(day: LocalDate, result: RecoveryResult) {
        db.scoresDao().upsertRecovery(
            RecoveryScoreEntity(
                dateEpochDay = day.toEpochDay(),
                score = result.score,
                confidence = result.confidence.name,
                hrvContributionPercent = result.contributions["HRV"],
                sleepContributionPercent = result.contributions["Sleep"],
                rhrContributionPercent = result.contributions["RHR"],
            )
        )
    }

    suspend fun saveSleep(day: LocalDate, result: SleepResult) {
        db.scoresDao().upsertSleep(
            SleepScoreEntity(
                dateEpochDay = day.toEpochDay(),
                score = result.score,
                durationMinutes = result.durationMinutes,
                efficiencyPercent = result.efficiencyPercent,
                sleepDebtMinutes = result.sleepDebtMinutes,
            )
        )
    }

    suspend fun saveStrain(day: LocalDate, result: StrainResult) {
        db.scoresDao().upsertStrain(
            StrainScoreEntity(dateEpochDay = day.toEpochDay(), score = result.score, trainingLoad = result.trainingLoad)
        )
    }

    suspend fun saveInsights(day: LocalDate, insights: List<Insight>) {
        insights.forEach {
            db.insightDao().insert(InsightEntity(dateEpochDay = day.toEpochDay(), text = it.text, category = it.category))
        }
    }

    suspend fun recoveryFor(day: LocalDate) = db.scoresDao().recoveryForDay(day.toEpochDay())
    suspend fun sleepFor(day: LocalDate) = db.scoresDao().sleepForDay(day.toEpochDay())
    suspend fun strainFor(day: LocalDate) = db.scoresDao().strainForDay(day.toEpochDay())

    fun observeRecovery(startDay: LocalDate, endDay: LocalDate) =
        db.scoresDao().observeRecoveryRange(startDay.toEpochDay(), endDay.toEpochDay())

    fun observeSleep(startDay: LocalDate, endDay: LocalDate) =
        db.scoresDao().observeSleepRange(startDay.toEpochDay(), endDay.toEpochDay())

    fun observeStrain(startDay: LocalDate, endDay: LocalDate) =
        db.scoresDao().observeStrainRange(startDay.toEpochDay(), endDay.toEpochDay())
}

/** Persists and retrieves rolling per-metric baselines learned by [com.vitalcore.app.domain.calculations.BaselineEngine]. */
@Singleton
class BaselineRepository @Inject constructor(private val db: VitalCoreDatabase) {

    suspend fun get(metricKey: String): BaselineStats? =
        db.baselineDao().get(metricKey)?.let { BaselineStats(it.mean, it.standardDeviation, it.sampleCount) }

    suspend fun save(metricKey: String, stats: BaselineStats, today: LocalDate) {
        db.baselineDao().upsert(
            UserBaselineEntity(
                metricKey = metricKey,
                mean = stats.mean,
                standardDeviation = stats.standardDeviation,
                sampleCount = stats.sampleCount,
                lastUpdatedEpochDay = today.toEpochDay(),
            )
        )
    }

    suspend fun getAll(): Map<String, BaselineStats> =
        db.baselineDao().getAll().associate { it.metricKey to BaselineStats(it.mean, it.standardDeviation, it.sampleCount) }

    suspend fun clearAll() = db.baselineDao().deleteAll()
}
