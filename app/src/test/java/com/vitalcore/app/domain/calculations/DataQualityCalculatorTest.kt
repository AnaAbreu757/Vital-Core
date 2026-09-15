package com.vitalcore.app.domain.calculations

import com.vitalcore.app.domain.model.DailyActivity
import com.vitalcore.app.domain.model.DailyHealthSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class DataQualityCalculatorTest {

    @Test
    fun `all signals missing yields zero percent`() {
        val snapshot = DailyHealthSnapshot(Instant.EPOCH, null, null, null, null, null, null, null)
        val result = DataQualityCalculator.evaluate(snapshot)
        assertEquals(0, result.percent)
        assertTrue(result.available.isEmpty())
    }

    @Test
    fun `all signals present yields one hundred percent`() {
        val snapshot = DailyHealthSnapshot(
            date = Instant.EPOCH,
            avgHrvMillis = 50.0,
            restingHeartRateBpm = 55,
            sleep = com.vitalcore.app.domain.model.SleepSession(Instant.EPOCH, Instant.EPOCH.plusSeconds(28800)),
            respiratoryRateBpm = 14.0,
            avgSpo2Percentage = 97.0,
            skinTempDeltaCelsius = 0.1,
            activity = DailyActivity(Instant.EPOCH, 8000, 300.0, 2200.0, 6000.0),
            exerciseSessions = listOf(
                com.vitalcore.app.domain.model.ExerciseSession(
                    Instant.EPOCH, Instant.EPOCH.plusSeconds(1800), "RUNNING", 140, 160, 250.0, 4000.0,
                )
            ),
        )
        val result = DataQualityCalculator.evaluate(snapshot)
        assertEquals(100, result.percent)
        assertTrue(result.missing.isEmpty())
    }

    @Test
    fun `partial data reports the specific missing signals`() {
        val snapshot = DailyHealthSnapshot(
            date = Instant.EPOCH,
            avgHrvMillis = 50.0,
            restingHeartRateBpm = null,
            sleep = null,
            respiratoryRateBpm = null,
            avgSpo2Percentage = null,
            skinTempDeltaCelsius = null,
            activity = null,
        )
        val result = DataQualityCalculator.evaluate(snapshot)
        assertTrue("HRV" in result.available)
        assertTrue("Sleep" in result.missing)
        assertTrue("Resting heart rate" in result.missing)
    }
}
