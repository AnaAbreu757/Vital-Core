package com.vitalcore.app.domain.calculations

import com.vitalcore.app.domain.model.DailyHealthSnapshot
import kotlin.math.roundToInt

data class Insight(val text: String, val category: String)

/**
 * Rule-based insights engine. Every generated sentence is either a plain
 * factual comparison ("X is Y% above your baseline") or, once enough history
 * exists, a correlation phrased with "associated with" — never "causes" or
 * "because of". See ALGORITHMS.md.
 */
object InsightsEngine {

    fun dailyInsights(
        today: DailyHealthSnapshot,
        baselines: Map<String, BaselineStats>,
        yesterdaySleepDurationMinutes: Long?,
        sleepDurationBaselineMinutes: Double?,
    ): List<Insight> {
        val insights = mutableListOf<Insight>()

        today.avgHrvMillis?.let { hrv ->
            baselines["hrv"]?.percentDelta(hrv)?.let { delta ->
                if (kotlin.math.abs(delta) >= 5) {
                    val direction = if (delta > 0) "above" else "below"
                    insights += Insight("Your HRV is ${kotlin.math.abs(delta).roundToInt()}% $direction your baseline.", "hrv")
                }
            }
        }

        if (yesterdaySleepDurationMinutes != null && sleepDurationBaselineMinutes != null) {
            val diff = (sleepDurationBaselineMinutes - yesterdaySleepDurationMinutes).roundToInt()
            if (diff > 15) {
                insights += Insight("You slept ${diff} minutes less than usual.", "sleep")
            } else if (diff < -15) {
                insights += Insight("You slept ${-diff} minutes more than usual.", "sleep")
            }
        }

        today.restingHeartRateBpm?.let { rhr ->
            baselines["resting_heart_rate"]?.let { baseline ->
                val z = baseline.deviation(rhr.toDouble())
                if (z != null && z > 1.0) {
                    insights += Insight("Your resting heart rate is higher than your recent baseline.", "rhr")
                }
            }
        }

        baselines["training_load"]?.let { baseline ->
            if (baseline.mean > 0 && baseline.sampleCount >= 3) {
                insights += Insight("You have accumulated meaningful training load over the last few days.", "strain")
            }
        }

        return insights
    }

    /**
     * Correlation-style insight, only emitted once there's enough paired
     * history (>= 14 days) to say anything responsible. Deliberately uses
     * "associated with" language and never claims causality.
     */
    fun correlationInsight(pairedDayCount: Int, lowSleepDays: Int, lowSleepFollowedByLowHrv: Int): Insight? {
        if (pairedDayCount < 14 || lowSleepDays < 4) return null
        val rate = lowSleepFollowedByLowHrv.toDouble() / lowSleepDays
        if (rate < 0.6) return null
        return Insight(
            "Days with less than 7 hours of sleep are associated with lower HRV the next morning, based on your recent history.",
            "pattern",
        )
    }
}
