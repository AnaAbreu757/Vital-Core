package com.vitalcore.app.data.repositories

import com.vitalcore.app.data.database.VitalCoreDatabase
import com.vitalcore.app.domain.calculations.StreakCalculator
import com.vitalcore.app.domain.calculations.StreakResult
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Derives the streak from every day that has a Recovery score on record — see StreakCalculator's doc comment for what counts as "active". */
@Singleton
class StreakRepository @Inject constructor(private val db: VitalCoreDatabase) {

    suspend fun currentStreak(): StreakResult {
        val today = LocalDate.now()
        val allRecovery = db.scoresDao().observeRecoveryRange(-365_000, today.toEpochDay()).first()
        val activeDays = allRecovery.map { LocalDate.ofEpochDay(it.dateEpochDay) }
        return StreakCalculator.calculate(activeDays, today)
    }
}
