package com.vitalcore.app.domain.calculations

import com.vitalcore.app.domain.model.DailyActivity
import com.vitalcore.app.domain.model.ExerciseSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class StrainCalculatorTest {

    @Test
    fun `no exercise and no activity yields zero strain`() {
        val result = StrainCalculator.calculate(emptyList(), null)
        assertEquals(0.0, result.score, 0.01)
    }

    @Test
    fun `strain is always within the 0 to 21 scale`() {
        val huge = ExerciseSession(
            start = Instant.EPOCH,
            end = Instant.EPOCH.plus(10, ChronoUnit.HOURS),
            activityType = "RUNNING",
            avgHeartRate = 190,
            maxHeartRate = 195,
            calories = 5000.0,
            distanceMeters = 50000.0,
        )
        val result = StrainCalculator.calculate(listOf(huge), DailyActivity(Instant.EPOCH, 50000, 5000.0, 6000.0, 40000.0))
        assertTrue(result.score in 0.0..21.0)
    }

    @Test
    fun `higher intensity exercise produces more strain than lower intensity of the same duration`() {
        fun session(avgHr: Int) = ExerciseSession(
            start = Instant.EPOCH,
            end = Instant.EPOCH.plus(30, ChronoUnit.MINUTES),
            activityType = "RUNNING",
            avgHeartRate = avgHr,
            maxHeartRate = avgHr + 10,
            calories = 200.0,
            distanceMeters = 4000.0,
        )
        val low = StrainCalculator.calculate(listOf(session(110)), null)
        val high = StrainCalculator.calculate(listOf(session(175)), null)
        assertTrue(high.score > low.score)
    }

    @Test
    fun `missing heart rate still produces a non-zero mild load rather than crashing`() {
        val session = ExerciseSession(
            start = Instant.EPOCH,
            end = Instant.EPOCH.plus(30, ChronoUnit.MINUTES),
            activityType = "WALKING",
            avgHeartRate = null,
            maxHeartRate = null,
            calories = null,
            distanceMeters = null,
        )
        val result = StrainCalculator.calculate(listOf(session), null)
        assertTrue(result.trainingLoad > 0.0)
    }

    @Test
    fun `daily non-exercise activity alone contributes some strain`() {
        val activity = DailyActivity(Instant.EPOCH, steps = 15000, activeCalories = null, totalCalories = 2200.0, distanceMeters = null)
        val result = StrainCalculator.calculate(emptyList(), activity)
        assertTrue(result.score > 0.0)
    }
}
