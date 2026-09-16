package com.vitalcore.app.domain.calculations

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StreakCalculatorTest {

    private val today = LocalDate.of(2026, 9, 11)

    @Test
    fun `no active days yields zero streaks`() {
        val result = StreakCalculator.calculate(emptyList(), today)
        assertEquals(0, result.currentStreak)
        assertEquals(0, result.longestStreak)
    }

    @Test
    fun `consecutive days up to today give a matching current streak`() {
        val days = listOf(today.minusDays(2), today.minusDays(1), today)
        val result = StreakCalculator.calculate(days, today)
        assertEquals(3, result.currentStreak)
        assertEquals(3, result.longestStreak)
    }

    @Test
    fun `streak still counts if yesterday is the last active day (today not logged yet)`() {
        val days = listOf(today.minusDays(3), today.minusDays(2), today.minusDays(1))
        val result = StreakCalculator.calculate(days, today)
        assertEquals(3, result.currentStreak)
    }

    @Test
    fun `a gap breaks the current streak but not the longest`() {
        val days = listOf(
            today.minusDays(10), today.minusDays(9), today.minusDays(8), today.minusDays(7), // longest run of 4
            today.minusDays(1), today, // current run of 2
        )
        val result = StreakCalculator.calculate(days, today)
        assertEquals(2, result.currentStreak)
        assertEquals(4, result.longestStreak)
    }

    @Test
    fun `a day with no data two or more days ago breaks the current streak entirely`() {
        val days = listOf(today.minusDays(5), today.minusDays(4))
        val result = StreakCalculator.calculate(days, today)
        assertEquals(0, result.currentStreak)
    }

    @Test
    fun `duplicate dates are not double-counted`() {
        val days = listOf(today, today, today.minusDays(1), today.minusDays(1))
        val result = StreakCalculator.calculate(days, today)
        assertEquals(2, result.currentStreak)
        assertEquals(2, result.longestStreak)
    }
}
