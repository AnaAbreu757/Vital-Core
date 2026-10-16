package com.vitalcore.app.domain.calculations

import kotlin.math.roundToInt

data class BiologicalAgeResult(
    val chronologicalAge: Int,
    val estimatedBiologicalAge: Int,
    /** >1.0 means aging faster than chronological pace, <1.0 means slower. 1.0 = neutral. */
    val paceOfAging: Double,
    val hasEnoughData: Boolean,
)

/**
 * A simplified, heuristic "biological age" estimate — inspired by the
 * concept WHOOP and similar products surface, but NOT a clinical or
 * laboratory measurement (true biological age estimation uses things like
 * DNA methylation, which no wearable can measure). This is a transparent,
 * rule-based adjustment to chronological age based on three everyday
 * wellness signals the app already tracks:
 *  - Recovery average (reflects HRV/RHR relative to the user's own baseline)
 *  - Average sleep duration vs. the commonly recommended 7-9h range
 *  - Average daily steps vs. commonly cited activity guidelines
 *
 * Each factor contributes a bounded -3..+3 year adjustment; the total is
 * clamped to ±15 years so a few rough days never produces an absurd
 * result. Always present this to the user as an estimate, never a fact.
 */
object BiologicalAgeCalculator {

    fun calculate(
        chronologicalAge: Int?,
        avgRecoveryScore: Double?,
        avgSleepHours: Double?,
        avgDailySteps: Double?,
    ): BiologicalAgeResult? {
        if (chronologicalAge == null) return null
        val signalsPresent = listOf(avgRecoveryScore, avgSleepHours, avgDailySteps).count { it != null }
        if (signalsPresent == 0) {
            return BiologicalAgeResult(chronologicalAge, chronologicalAge, 1.0, hasEnoughData = false)
        }

        var adjustment = 0.0
        avgRecoveryScore?.let { r ->
            adjustment += when {
                r >= 70 -> -3.0
                r >= 50 -> -1.0
                r >= 30 -> 1.0
                else -> 3.0
            }
        }
        avgSleepHours?.let { h ->
            adjustment += when {
                h in 7.0..9.0 -> -2.0
                h in 6.0..10.0 -> 0.0
                else -> 2.0
            }
        }
        avgDailySteps?.let { s ->
            adjustment += when {
                s >= 8000 -> -2.0
                s >= 5000 -> 0.0
                else -> 2.0
            }
        }

        val clampedAdjustment = adjustment.coerceIn(-15.0, 15.0)
        val biologicalAge = (chronologicalAge + clampedAdjustment).roundToInt().coerceAtLeast(1)
        val pace = biologicalAge.toDouble() / chronologicalAge.toDouble()

        return BiologicalAgeResult(
            chronologicalAge = chronologicalAge,
            estimatedBiologicalAge = biologicalAge,
            paceOfAging = pace,
            hasEnoughData = true,
        )
    }
}
