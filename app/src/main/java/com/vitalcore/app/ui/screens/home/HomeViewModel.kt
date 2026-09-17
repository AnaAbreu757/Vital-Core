package com.vitalcore.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalcore.app.data.repositories.BaselineRepository
import com.vitalcore.app.data.repositories.HealthDataRepository
import com.vitalcore.app.data.repositories.ScoreRepository
import com.vitalcore.app.domain.calculations.BaselineEngine
import com.vitalcore.app.domain.calculations.DataQualityCalculator
import com.vitalcore.app.domain.calculations.Insight
import com.vitalcore.app.domain.calculations.InsightsEngine
import com.vitalcore.app.domain.calculations.RecoveryCalculator
import com.vitalcore.app.domain.calculations.SleepCalculator
import com.vitalcore.app.domain.calculations.StrainCalculator
import com.vitalcore.app.notifications.AlertNotifier
import com.vitalcore.app.auth.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

data class HomeUiState(
    val loading: Boolean = true,
    val recoveryScore: Int? = null,
    val recoveryConfidence: String? = null,
    val sleepScore: Int? = null,
    val strainScore: Double? = null,
    val topInsight: String? = null,
    val steps: Long? = null,
    val calories: Double? = null,
    val hrvMillis: Double? = null,
    val restingHeartRateBpm: Int? = null,
    val dataQualityPercent: Int = 0,
)

/**
 * Drives the Home dashboard: pulls the last 30 days of snapshots, builds
 * baselines, runs Recovery/Sleep/Strain for today, and surfaces the top
 * insight. This is the first screen wired to the real calculation engine
 * (Milestone 8) — Recovery/Sleep/Activity/Health screens follow the same
 * pattern with their own, more detailed ViewModels.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val healthDataRepository: HealthDataRepository,
    private val scoreRepository: ScoreRepository,
    private val baselineRepository: BaselineRepository,
    private val alertNotifier: AlertNotifier,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)

            val today = LocalDate.now(ZoneOffset.UTC)
            val historyStart = today.minusDays(30)

            healthDataRepository.sync(
                historyStart.atStartOfDay(ZoneOffset.UTC).toInstant(),
                today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant(),
            )

            val history = healthDataRepository.snapshotsForRange(historyStart, today)
            val todaySnapshot = history.last()
            val yesterdaySnapshot = history.getOrNull(history.size - 2)

            val hrvBaseline = BaselineEngine.compute(
                BaselineEngine.withoutOutliers(history.mapNotNull { it.avgHrvMillis })
            )
            val rhrBaseline = BaselineEngine.compute(
                BaselineEngine.withoutOutliers(history.mapNotNull { it.restingHeartRateBpm?.toDouble() })
            )
            val respiratoryBaseline = BaselineEngine.compute(history.mapNotNull { it.respiratoryRateBpm })
            val spo2Baseline = BaselineEngine.compute(history.mapNotNull { it.avgSpo2Percentage })
            val tempBaseline = BaselineEngine.compute(history.mapNotNull { it.skinTempDeltaCelsius })
            val sleepDurationBaseline = BaselineEngine.compute(history.mapNotNull { it.sleep?.durationMinutes?.toDouble() })

            val baselines = buildMap {
                hrvBaseline?.let { put("hrv", it) }
                rhrBaseline?.let { put("resting_heart_rate", it) }
                respiratoryBaseline?.let { put("respiratory_rate", it) }
                spo2Baseline?.let { put("spo2", it) }
                tempBaseline?.let { put("temperature", it) }
            }

            val sleepResult = SleepCalculator.calculate(
                session = yesterdaySnapshot?.sleep,
                durationBaseline = sleepDurationBaseline,
                recentBedtimeVarianceMinutes = null,
            )

            val recoveryResult = RecoveryCalculator.calculate(
                today = todaySnapshot,
                yesterdaySleepScore = sleepResult.score,
                baselines = baselines,
                recentTrainingLoad = null,
            )

            val strainResult = StrainCalculator.calculate(
                exercises = todaySnapshot.exerciseSessions,
                dailyActivity = todaySnapshot.activity,
            )

            scoreRepository.saveRecovery(today, recoveryResult)
            scoreRepository.saveSleep(today, sleepResult)
            scoreRepository.saveStrain(today, strainResult)
            baselines.forEach { (key, stats) -> baselineRepository.save(key, stats, today) }

            val insights: List<Insight> = InsightsEngine.dailyInsights(
                today = todaySnapshot,
                baselines = baselines,
                yesterdaySleepDurationMinutes = yesterdaySnapshot?.sleep?.durationMinutes,
                sleepDurationBaselineMinutes = sleepDurationBaseline?.mean,
            )
            scoreRepository.saveInsights(today, insights)

            val dataQuality = DataQualityCalculator.evaluate(todaySnapshot)

            alertNotifier.maybeNotifyLowRecovery(recoveryResult.score)
            alertNotifier.maybeNotifyHighStrain(strainResult.score)

            // Best-effort: no-ops silently if not signed in / Firebase not configured.
            runCatching { profileRepository.publishProfileAndStats() }

            _uiState.value = HomeUiState(
                loading = false,
                recoveryScore = recoveryResult.score,
                recoveryConfidence = recoveryResult.confidence.name,
                sleepScore = sleepResult.score,
                strainScore = strainResult.score,
                topInsight = insights.firstOrNull()?.text,
                steps = todaySnapshot.activity?.steps,
                calories = todaySnapshot.activity?.totalCalories,
                hrvMillis = todaySnapshot.avgHrvMillis,
                restingHeartRateBpm = todaySnapshot.restingHeartRateBpm,
                dataQualityPercent = dataQuality.percent,
            )
        }
    }
}
