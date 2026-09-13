package com.vitalcore.app.domain.calculations

import com.vitalcore.app.domain.model.SleepSession
import com.vitalcore.app.domain.model.SleepStageType
import java.time.Duration
import kotlin.math.roundToInt

data class SleepResult(
    val score: Int,
    val durationMinutes: Long,
    val efficiencyPercent: Int?,
    val sleepDebtMinutes: Long?,
)

/**
 * Original VitalCore Sleep algorithm. Combines duration (vs. a target and vs.
 * personal baseline), bed/wake-time consistency, sleep debt, and efficiency.
 * Sleep stages are only used when the source device actually reports them —
 * never fabricated, per the "never invent sleep stages" rule.
 */
object SleepCalculator {

    private const val TARGET_SLEEP_MINUTES = 8 * 60L

    fun calculate(
        session: SleepSession?,
        durationBaseline: BaselineStats?,
        recentBedtimeVarianceMinutes: Double?,
    ): SleepResult {
        if (session == null) {
            return SleepResult(score = 0, durationMinutes = 0, efficiencyPercent = null, sleepDebtMinutes = null)
        }

        val durationMinutes = session.durationMinutes
        val durationScore = durationComponent(durationMinutes)

        val consistencyScore = recentBedtimeVarianceMinutes?.let { variance ->
            // Lower variance (minutes) is better; 0 min variance = perfect, 120+ min = poor.
            (1.0 - (variance / 120.0)).coerceIn(0.0, 1.0)
        } ?: 0.5 // neutral if we don't have enough history yet

        val efficiencyPercent = efficiency(session)
        val efficiencyScore = efficiencyPercent?.let { it / 100.0 } ?: 0.5

        val debtMinutes = durationBaseline?.let { baseline ->
            max(0L, (baseline.mean - durationMinutes).roundToInt().toLong())
        }
        val debtScore = debtMinutes?.let { (1.0 - (it / 180.0)).coerceIn(0.0, 1.0) } ?: 0.5

        // Weighting: duration matters most, then efficiency, then consistency and debt.
        val weighted = durationScore * 0.40 + efficiencyScore * 0.25 + consistencyScore * 0.20 + debtScore * 0.15
        val score = (weighted * 100).roundToInt().coerceIn(0, 100)

        return SleepResult(score, durationMinutes, efficiencyPercent, debtMinutes)
    }

    private fun durationComponent(minutes: Long): Double {
        val ratio = minutes.toDouble() / TARGET_SLEEP_MINUTES
        // Symmetric penalty for both too little and (mildly) too much sleep.
        return when {
            ratio >= 1.0 -> (1.0 - (ratio - 1.0).coerceAtMost(0.5)).coerceIn(0.0, 1.0)
            else -> ratio.coerceIn(0.0, 1.0)
        }
    }

    /** Efficiency = time actually asleep (non-AWAKE stages) / time in bed. Null if the device didn't report stages. */
    private fun efficiency(session: SleepSession): Int? {
        if (session.stages.isEmpty()) return null
        val totalInBed = Duration.between(session.start, session.end).toMinutes()
        if (totalInBed <= 0) return null
        val asleep = session.stages.filter { it.stage != SleepStageType.AWAKE }
            .sumOf { Duration.between(it.start, it.end).toMinutes() }
        return ((asleep.toDouble() / totalInBed) * 100).roundToInt().coerceIn(0, 100)
    }

    private fun max(a: Long, b: Long) = if (a > b) a else b
}
