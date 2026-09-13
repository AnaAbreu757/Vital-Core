package com.vitalcore.app.domain.calculations

import com.vitalcore.app.domain.model.DailyActivity
import com.vitalcore.app.domain.model.ExerciseSession
import java.time.Duration
import kotlin.math.ln
import kotlin.math.min

data class StrainResult(
    /** 0..21 scale, deliberately not a copy of WHOOP's proprietary strain curve. */
    val score: Double,
    val trainingLoad: Double,
)

/**
 * Original VitalCore Strain algorithm. Accumulates a "load" score across the
 * day from exercise heart-rate zones, exercise duration, and daily
 * non-exercise activity (steps/calories), then maps that load onto a 0-21
 * scale using a logarithmic curve — meaning strain grows quickly at low
 * effort and flattens out at very high effort, similar in spirit (not
 * formula) to how cardiovascular load accumulates in real training.
 */
object StrainCalculator {

    // Approximate max HR used only when a personal max isn't known; replaced by
    // a measured value once VO2 max / user profile data exists (later milestone).
    private const val DEFAULT_MAX_HR = 190

    fun calculate(
        exercises: List<ExerciseSession>,
        dailyActivity: DailyActivity?,
        userMaxHeartRate: Int? = null,
    ): StrainResult {
        val maxHr = userMaxHeartRate ?: DEFAULT_MAX_HR

        val exerciseLoad = exercises.sumOf { session ->
            val minutes = Duration.between(session.start, session.end).toMinutes().coerceAtLeast(0)
            val avgHr = session.avgHeartRate ?: return@sumOf minutes * 0.5 // unknown intensity: mild default load/min
            val hrReserveFraction = (avgHr.toDouble() / maxHr).coerceIn(0.0, 1.5)
            // Zone multiplier: low effort barely counts, high effort counts a lot.
            val zoneMultiplier = when {
                hrReserveFraction < 0.5 -> 0.2
                hrReserveFraction < 0.6 -> 0.6
                hrReserveFraction < 0.7 -> 1.0
                hrReserveFraction < 0.8 -> 1.6
                hrReserveFraction < 0.9 -> 2.4
                else -> 3.2
            }
            minutes * zoneMultiplier
        }

        val activityLoad = dailyActivity?.let { activity ->
            val stepsLoad = (activity.steps ?: 0L) / 1000.0 // ~1 point per 1000 steps
            val caloriesLoad = (activity.totalCalories ?: 0.0) / 200.0 // ~1 point per 200 kcal
            stepsLoad + caloriesLoad
        } ?: 0.0

        val totalLoad = exerciseLoad + activityLoad

        // Logarithmic mapping onto 0..21: score = 21 * (ln(1 + load/k) / ln(1 + maxLoad/k))
        val k = 15.0
        val maxLoadReference = 400.0
        val score = 21.0 * (ln(1 + totalLoad / k) / ln(1 + maxLoadReference / k))

        return StrainResult(score = min(21.0, score).coerceAtLeast(0.0), trainingLoad = totalLoad)
    }
}
