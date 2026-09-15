package com.vitalcore.app.domain.calculations

import com.vitalcore.app.domain.model.DailyHealthSnapshot
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class InsightsEngineTest {

    private fun snapshot(hrv: Double? = null, rhr: Int? = null) = DailyHealthSnapshot(
        date = Instant.EPOCH,
        avgHrvMillis = hrv,
        restingHeartRateBpm = rhr,
        sleep = null,
        respiratoryRateBpm = null,
        avgSpo2Percentage = null,
        skinTempDeltaCelsius = null,
        activity = null,
    )

    @Test
    fun `no insights when nothing deviates from baseline`() {
        val baseline = BaselineEngine.compute(List(20) { 50.0 })!!
        val insights = InsightsEngine.dailyInsights(
            today = snapshot(hrv = 50.0),
            baselines = mapOf("hrv" to baseline),
            yesterdaySleepDurationMinutes = null,
            sleepDurationBaselineMinutes = null,
        )
        assertTrue(insights.none { it.category == "hrv" })
    }

    @Test
    fun `hrv insight fires when meaningfully above baseline`() {
        val baseline = BaselineEngine.compute(List(20) { 50.0 })!!
        val insights = InsightsEngine.dailyInsights(
            today = snapshot(hrv = 60.0),
            baselines = mapOf("hrv" to baseline),
            yesterdaySleepDurationMinutes = null,
            sleepDurationBaselineMinutes = null,
        )
        val hrvInsight = insights.firstOrNull { it.category == "hrv" }
        assertTrue(hrvInsight != null)
        assertTrue(hrvInsight!!.text.contains("above"))
    }

    @Test
    fun `insight text never asserts causality`() {
        val baseline = BaselineEngine.compute(List(20) { 50.0 })!!
        val insights = InsightsEngine.dailyInsights(
            today = snapshot(hrv = 60.0),
            baselines = mapOf("hrv" to baseline),
            yesterdaySleepDurationMinutes = null,
            sleepDurationBaselineMinutes = null,
        )
        insights.forEach { assertFalse(it.text.contains(" because ", ignoreCase = true)) }
        insights.forEach { assertFalse(it.text.contains(" causes ", ignoreCase = true)) }
    }

    @Test
    fun `correlation insight requires at least 14 paired days and a strong pattern`() {
        assertNull(InsightsEngine.correlationInsight(pairedDayCount = 10, lowSleepDays = 5, lowSleepFollowedByLowHrv = 5))
        assertNull(InsightsEngine.correlationInsight(pairedDayCount = 20, lowSleepDays = 5, lowSleepFollowedByLowHrv = 2))
        val insight = InsightsEngine.correlationInsight(pairedDayCount = 20, lowSleepDays = 5, lowSleepFollowedByLowHrv = 4)
        assertTrue(insight != null)
        assertTrue(insight!!.text.contains("associated with"))
    }
}
