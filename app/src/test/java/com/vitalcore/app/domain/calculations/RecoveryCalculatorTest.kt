package com.vitalcore.app.domain.calculations

import com.vitalcore.app.domain.model.DailyHealthSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class RecoveryCalculatorTest {

    private fun snapshot(
        hrv: Double? = null,
        rhr: Int? = null,
        respRate: Double? = null,
        spo2: Double? = null,
        temp: Double? = null,
    ) = DailyHealthSnapshot(
        date = Instant.EPOCH,
        avgHrvMillis = hrv,
        restingHeartRateBpm = rhr,
        sleep = null,
        respiratoryRateBpm = respRate,
        avgSpo2Percentage = spo2,
        skinTempDeltaCelsius = temp,
        activity = null,
    )

    @Test
    fun `no data at all yields zero score and low confidence`() {
        val result = RecoveryCalculator.calculate(
            today = snapshot(),
            yesterdaySleepScore = null,
            baselines = emptyMap(),
            recentTrainingLoad = null,
        )
        assertEquals(0, result.score)
        assertEquals(ConfidenceLevel.LOW, result.confidence)
        assertEquals(0, result.dataQualityPercent)
    }

    @Test
    fun `hrv above baseline increases score above neutral`() {
        val baseline = BaselineEngine.compute(List(20) { 50.0 })!!
        val result = RecoveryCalculator.calculate(
            today = snapshot(hrv = 70.0),
            yesterdaySleepScore = null,
            baselines = mapOf("hrv" to baseline),
            recentTrainingLoad = null,
        )
        assertTrue("expected score above 50 for above-baseline HRV, got ${result.score}", result.score > 50)
    }

    @Test
    fun `hrv below baseline decreases score below neutral`() {
        val baseline = BaselineEngine.compute(List(20) { 50.0 })!!
        val result = RecoveryCalculator.calculate(
            today = snapshot(hrv = 30.0),
            yesterdaySleepScore = null,
            baselines = mapOf("hrv" to baseline),
            recentTrainingLoad = null,
        )
        assertTrue("expected score below 50 for below-baseline HRV, got ${result.score}", result.score < 50)
    }

    @Test
    fun `higher resting heart rate than baseline is penalized`() {
        val baseline = BaselineEngine.compute(List(20) { 55.0 })!!
        val result = RecoveryCalculator.calculate(
            today = snapshot(rhr = 75),
            yesterdaySleepScore = null,
            baselines = mapOf("resting_heart_rate" to baseline),
            recentTrainingLoad = null,
        )
        assertTrue(result.score < 50)
    }

    @Test
    fun `more available signals increase confidence`() {
        val baseline = BaselineEngine.compute(List(20) { 50.0 })!!
        val partial = RecoveryCalculator.calculate(
            today = snapshot(hrv = 50.0),
            yesterdaySleepScore = null,
            baselines = mapOf("hrv" to baseline),
            recentTrainingLoad = null,
        )
        val fuller = RecoveryCalculator.calculate(
            today = snapshot(hrv = 50.0, rhr = 55, respRate = 14.0, spo2 = 97.0, temp = 0.0),
            yesterdaySleepScore = 80,
            baselines = mapOf(
                "hrv" to baseline, "resting_heart_rate" to baseline,
                "respiratory_rate" to baseline, "spo2" to baseline, "temperature" to baseline,
            ),
            recentTrainingLoad = null,
        )
        assertTrue(fuller.dataQualityPercent > partial.dataQualityPercent)
    }

    @Test
    fun `score is always clamped between 0 and 100`() {
        val baseline = BaselineEngine.compute(List(20) { 50.0 })!!
        val result = RecoveryCalculator.calculate(
            today = snapshot(hrv = 100000.0),
            yesterdaySleepScore = null,
            baselines = mapOf("hrv" to baseline),
            recentTrainingLoad = null,
        )
        assertTrue(result.score in 0..100)
    }
}
