package com.vitalcore.app.domain.calculations

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class StreakResult(
    val currentStreak: Int,
    val longestStreak: Int,
    val activeDays: Set<LocalDate>,
)

/**
 * Pure-Kotlin streak calculator. A day counts as "active" if the app has any
 * computed Recovery score for it (i.e. Health Connect data synced and the
 * calculation engine ran) — the same definition used for the streak
 * calendar and the value mirrored to Firestore's public_stats doc.
 */
object StreakCalculator {

    fun calculate(activeDays: List<LocalDate>, today: LocalDate = LocalDate.now()): StreakResult {
        if (activeDays.isEmpty()) return StreakResult(0, 0, emptySet())
        val sorted = activeDays.distinct().sorted()
        val daySet = sorted.toSet()

        var current = 0
        var cursor = if (daySet.contains(today)) today else today.minusDays(1)
        while (daySet.contains(cursor)) {
            current++
            cursor = cursor.minusDays(1)
        }

        var longest = 1
        var run = 1
        for (i in 1 until sorted.size) {
            val gap = ChronoUnit.DAYS.between(sorted[i - 1], sorted[i])
            run = if (gap == 1L) run + 1 else 1
            if (run > longest) longest = run
        }

        return StreakResult(current, longest, daySet)
    }
}
