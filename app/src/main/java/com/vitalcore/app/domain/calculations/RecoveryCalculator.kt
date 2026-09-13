package com.vitalcore.app.domain.calculations

import com.vitalcore.app.domain.model.DailyHealthSnapshot
import kotlin.math.max
import kotlin.math.min

enum class ConfidenceLevel { LOW, MEDIUM, HIGH }

/** Per-component weight, configurable in Settings (Milestone 12). Must sum to 1.0. */
data class RecoveryWeights(
    val hrv: Double = 0.35,
    val restingHeartRate: Double = 0.20,
    val sleep: Double = 0.20,
    val respiratoryRate: Double = 0.10,
    val spo2: Double = 0.05,
    val temperature: Double = 0.05,
    val trainingLoad: Double = 0.05,
) {
    init { require(kotlin.math.abs((hrv + restingHeartRate + sleep + respiratoryRate + spo2 + temperature + trainingLoad) - 1.0) < 0.01) }
}

data class RecoveryResult(
    val score: Int,
    val confidence: ConfidenceLevel,
    /** Human-facing breakdown, e.g. "HRV" -> +12.0 (percent vs baseline). */
    val contributions: Map<String, Double>,
    val dataQualityPercent: Int,
)

/**
 * Original VitalCore Recovery algorithm — not a reproduction of WHOOP's or
 * any other proprietary formula. Combines each available signal's deviation
 * from the user's personal baseline into a single 0-100 score, weighted per
 * [RecoveryWeights]. Missing signals are excluded from the score entirely
 * (their weight is redistributed proportionally across the signals that ARE
 * present) rather than treated as neutral/average — this keeps the score
 * honest about what it actually knows.
 */
object RecoveryCalculator {

    fun calculate(
        today: DailyHealthSnapshot,
        yesterdaySleepScore: Int?,
        baselines: Map<String, BaselineStats>,
        recentTrainingLoad: Double?,
        weights: RecoveryWeights = RecoveryWeights(),
    ): RecoveryResult {
        // Each entry: label -> (normalized 0..1 "goodness" for today, weight)
        val components = mutableListOf<Triple<String, Double, Double>>()

        today.avgHrvMillis?.let { hrv ->
            baselines["hrv"]?.let { baseline ->
                val z = baseline.deviation(hrv)
                if (z != null) components += Triple("HRV", zToGoodness(z, higherIsBetter = true), weights.hrv)
            }
        }
        today.restingHeartRateBpm?.let { rhr ->
            baselines["resting_heart_rate"]?.let { baseline ->
                val z = baseline.deviation(rhr.toDouble())
                if (z != null) components += Triple("RHR", zToGoodness(z, higherIsBetter = false), weights.restingHeartRate)
            }
        }
        yesterdaySleepScore?.let { sleepScore ->
            components += Triple("Sleep", (sleepScore / 100.0).coerceIn(0.0, 1.0), weights.sleep)
        }
        today.respiratoryRateBpm?.let { rr ->
            baselines["respiratory_rate"]?.let { baseline ->
                val z = baseline.deviation(rr)
                if (z != null) components += Triple("Respiratory rate", zToGoodness(z, higherIsBetter = false, dampen = true), weights.respiratoryRate)
            }
        }
        today.avgSpo2Percentage?.let { spo2 ->
            baselines["spo2"]?.let { baseline ->
                val z = baseline.deviation(spo2)
                if (z != null) components += Triple("SpO2", zToGoodness(z, higherIsBetter = true, dampen = true), weights.spo2)
            }
        }
        today.skinTempDeltaCelsius?.let { temp ->
            baselines["temperature"]?.let { baseline ->
                val z = baseline.deviation(temp)
                if (z != null) components += Triple("Temperature", zToGoodness(z, higherIsBetter = false, dampen = true), weights.temperature)
            }
        }
        recentTrainingLoad?.let { load ->
            baselines["training_load"]?.let { baseline ->
                val z = baseline.deviation(load)
                if (z != null) components += Triple("Training load", zToGoodness(z, higherIsBetter = false, dampen = true), weights.trainingLoad)
            }
        }

        if (components.isEmpty()) {
            return RecoveryResult(score = 0, confidence = ConfidenceLevel.LOW, contributions = emptyMap(), dataQualityPercent = 0)
        }

        val totalWeight = components.sumOf { it.third }
        val weightedGoodness = components.sumOf { (_, goodness, weight) -> goodness * (weight / totalWeight) }
        val score = (weightedGoodness * 100).toInt().coerceIn(0, 100)

        val contributions = components.associate { (label, goodness, _) ->
            label to ((goodness - 0.5) * 100.0) // rough "+/- % vs a neutral midpoint" for display
        }

        val dataQuality = ((components.size.toDouble() / 7.0) * 100).toInt().coerceIn(0, 100)
        val confidence = when {
            dataQuality >= 70 -> ConfidenceLevel.HIGH
            dataQuality >= 40 -> ConfidenceLevel.MEDIUM
            else -> ConfidenceLevel.LOW
        }

        return RecoveryResult(score, confidence, contributions, dataQuality)
    }

    /** Maps a z-score (deviation from baseline) to a 0..1 "goodness" value using a soft logistic-like curve so extreme outliers don't dominate. `dampen` further compresses the curve for signals we trust less as sole recovery indicators. */
    private fun zToGoodness(z: Double, higherIsBetter: Boolean, dampen: Boolean = false): Double {
        val signed = if (higherIsBetter) z else -z
        val scale = if (dampen) 3.0 else 2.0
        val clamped = max(-scale, min(scale, signed))
        return 0.5 + (clamped / scale) * 0.5
    }
}
