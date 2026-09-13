package com.vitalcore.app.domain.calculations

import com.vitalcore.app.domain.model.DailyHealthSnapshot

data class DataQualityResult(
    val percent: Int,
    val available: List<String>,
    val missing: List<String>,
)

/** Computes the "Data quality: 92% — ✓ HR ✓ Sleep ✓ Activity ⚠ HRV gaps" summary shown in Settings/Home. */
object DataQualityCalculator {
    private val TRACKED = listOf("Heart rate", "HRV", "Resting heart rate", "Sleep", "Respiratory rate", "SpO2", "Activity")

    fun evaluate(snapshot: DailyHealthSnapshot): DataQualityResult {
        val present = mutableListOf<String>()
        val missing = mutableListOf<String>()

        fun mark(label: String, hasData: Boolean) {
            if (hasData) present += label else missing += label
        }

        mark("Heart rate", snapshot.exerciseSessions.any { it.avgHeartRate != null })
        mark("HRV", snapshot.avgHrvMillis != null)
        mark("Resting heart rate", snapshot.restingHeartRateBpm != null)
        mark("Sleep", snapshot.sleep != null)
        mark("Respiratory rate", snapshot.respiratoryRateBpm != null)
        mark("SpO2", snapshot.avgSpo2Percentage != null)
        mark("Activity", snapshot.activity?.steps != null || snapshot.activity?.totalCalories != null)

        val percent = ((present.size.toDouble() / TRACKED.size) * 100).toInt()
        return DataQualityResult(percent, present, missing)
    }
}
