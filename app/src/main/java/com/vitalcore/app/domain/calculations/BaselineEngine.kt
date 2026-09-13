package com.vitalcore.app.domain.calculations

import kotlin.math.pow
import kotlin.math.sqrt

/** How far along a metric's personal baseline is, per the product spec:
 * <7 days = collecting, <14 = initial, <30 = still building, 30+ = robust. */
enum class BaselineMaturity { COLLECTING, INITIAL, BUILDING, ROBUST }

data class BaselineStats(
    val mean: Double,
    val standardDeviation: Double,
    val sampleCount: Int,
) {
    val maturity: BaselineMaturity
        get() = when {
            sampleCount < 7 -> BaselineMaturity.COLLECTING
            sampleCount < 14 -> BaselineMaturity.INITIAL
            sampleCount < 30 -> BaselineMaturity.BUILDING
            else -> BaselineMaturity.ROBUST
        }

    /** How many standard deviations `value` is from this baseline, or null if there's no usable spread yet. */
    fun deviation(value: Double): Double? {
        if (standardDeviation <= 0.0) return null
        return (value - mean) / standardDeviation
    }

    /** Percent difference from baseline mean, the form insights/recovery breakdowns show ("HRV +12%"). */
    fun percentDelta(value: Double): Double? {
        if (mean == 0.0) return null
        return ((value - mean) / mean) * 100.0
    }
}

/**
 * Pure-Kotlin rolling baseline calculator. Takes a metric's history (oldest
 * first) and produces mean/stddev over a bounded trailing window, per metric.
 * A baseline is never invented: with 0 samples, [BaselineStats] simply isn't
 * returned (see [compute] returning null).
 *
 * Window: last 60 days max, so the baseline adapts slowly over time rather
 * than being fixed forever from the first month.
 */
object BaselineEngine {
    private const val MAX_WINDOW_DAYS = 60

    fun compute(history: List<Double>): BaselineStats? {
        val window = history.takeLast(MAX_WINDOW_DAYS)
        if (window.isEmpty()) return null
        val mean = window.average()
        val variance = window.sumOf { (it - mean).pow(2) } / window.size
        return BaselineStats(mean = mean, standardDeviation = sqrt(variance), sampleCount = window.size)
    }

    /** Rejects obvious outliers (>4 std devs from the running mean) before folding a new value into history, so a single bad sensor reading can't skew the baseline. */
    fun withoutOutliers(history: List<Double>): List<Double> {
        if (history.size < 5) return history
        val stats = compute(history) ?: return history
        if (stats.standardDeviation <= 0.0) return history
        return history.filter { kotlin.math.abs((it - stats.mean) / stats.standardDeviation) <= 4.0 }
    }
}
