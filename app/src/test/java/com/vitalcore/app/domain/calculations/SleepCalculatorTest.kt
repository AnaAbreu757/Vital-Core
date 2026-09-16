package com.vitalcore.app.domain.calculations

import com.vitalcore.app.domain.model.SleepSession
import com.vitalcore.app.domain.model.SleepStageSegment
import com.vitalcore.app.domain.model.SleepStageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class SleepCalculatorTest {

    @Test
    fun `null session (missing data) yields zero score, never a fabricated value`() {
        val result = SleepCalculator.calculate(null, null, null)
        assertEquals(0, result.score)
        assertEquals(0L, result.durationMinutes)
        assertNull(result.efficiencyPercent)
    }

    @Test
    fun `session without stages has null efficiency, never invented`() {
        val start = Instant.EPOCH
        val session = SleepSession(start, start.plus(8, ChronoUnit.HOURS))
        val result = SleepCalculator.calculate(session, null, null)
        assertNull(result.efficiencyPercent)
        assertEquals(8 * 60L, result.durationMinutes)
    }

    @Test
    fun `session with stages computes efficiency from asleep time only`() {
        val start = Instant.EPOCH
        val end = start.plus(8, ChronoUnit.HOURS)
        val stages = listOf(
            SleepStageSegment(start, start.plus(30, ChronoUnit.MINUTES), SleepStageType.AWAKE),
            SleepStageSegment(start.plus(30, ChronoUnit.MINUTES), end, SleepStageType.DEEP),
        )
        val result = SleepCalculator.calculate(SleepSession(start, end, stages), null, null)
        // 7.5h asleep / 8h in bed = 93.75% -> rounds to 94
        assertEquals(94, result.efficiencyPercent)
    }

    @Test
    fun `8 hours of sleep near target duration scores well`() {
        val start = Instant.EPOCH
        val session = SleepSession(start, start.plus(8, ChronoUnit.HOURS))
        val result = SleepCalculator.calculate(session, null, null)
        assertTrue("expected a solid score for 8h sleep, got ${result.score}", result.score >= 60)
    }

    @Test
    fun `very short sleep scores poorly`() {
        val start = Instant.EPOCH
        val session = SleepSession(start, start.plus(3, ChronoUnit.HOURS))
        val result = SleepCalculator.calculate(session, null, null)
        assertTrue("expected a low score for 3h sleep, got ${result.score}", result.score < 50)
    }

    @Test
    fun `sleep debt is computed against baseline when available`() {
        val baseline = BaselineEngine.compute(List(20) { 480.0 })!! // 8h baseline
        val start = Instant.EPOCH
        val session = SleepSession(start, start.plus(6, ChronoUnit.HOURS)) // 360 min
        val result = SleepCalculator.calculate(session, baseline, null)
        assertEquals(120L, result.sleepDebtMinutes)
    }

    @Test
    fun `sleep debt is null without a duration baseline`() {
        val start = Instant.EPOCH
        val session = SleepSession(start, start.plus(6, ChronoUnit.HOURS))
        val result = SleepCalculator.calculate(session, null, null)
        assertNull(result.sleepDebtMinutes)
    }
}
