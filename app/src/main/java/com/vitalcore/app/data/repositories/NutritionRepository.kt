package com.vitalcore.app.data.repositories

import com.vitalcore.app.data.database.VitalCoreDatabase
import com.vitalcore.app.data.database.entities.FoodEntryEntity
import com.vitalcore.app.data.database.entities.WaterEntryEntity
import com.vitalcore.app.domain.model.FoodEntry
import com.vitalcore.app.domain.model.MealType
import com.vitalcore.app.domain.model.NutritionDailySummary
import com.vitalcore.app.domain.model.WaterEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local-only food and water logging. Nothing here ever leaves the device
 * except through the same JSON/CSV export path as the rest of the app's
 * data (see PrivacyExportManager) — logging what you eat and drink is just
 * as privacy-sensitive as any Health Connect signal, so it gets the same
 * local-first treatment.
 */
@Singleton
class NutritionRepository @Inject constructor(private val db: VitalCoreDatabase) {

    suspend fun logFood(
        time: Instant,
        name: String,
        calories: Int,
        proteinGrams: Double?,
        carbsGrams: Double?,
        fatGrams: Double?,
        mealType: MealType,
    ): Long = db.nutritionDao().insertFood(
        FoodEntryEntity(
            epochMillis = time.toEpochMilli(),
            name = name,
            calories = calories,
            proteinGrams = proteinGrams,
            carbsGrams = carbsGrams,
            fatGrams = fatGrams,
            mealType = mealType.name,
        )
    )

    suspend fun logWater(time: Instant, milliliters: Int): Long =
        db.nutritionDao().insertWater(WaterEntryEntity(epochMillis = time.toEpochMilli(), milliliters = milliliters))

    suspend fun deleteFood(id: Long) = db.nutritionDao().deleteFood(id)
    suspend fun deleteWater(id: Long) = db.nutritionDao().deleteWater(id)

    /** Observes the full nutrition summary for one calendar day (UTC), recomputed whenever entries change. */
    fun observeDailySummary(day: LocalDate, waterGoalMilliliters: Int = 2000): Flow<NutritionDailySummary> {
        val start = day.atStartOfDay(ZoneOffset.UTC).toInstant()
        val end = day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()

        val foodFlow = db.nutritionDao().observeFoodBetween(start.toEpochMilli(), end.toEpochMilli())
        val waterFlow = db.nutritionDao().observeWaterBetween(start.toEpochMilli(), end.toEpochMilli())

        return foodFlow.combine(waterFlow) { foodEntities, waterEntities ->
            val foodEntries = foodEntities.map {
                FoodEntry(
                    id = it.id,
                    time = Instant.ofEpochMilli(it.epochMillis),
                    name = it.name,
                    calories = it.calories,
                    proteinGrams = it.proteinGrams,
                    carbsGrams = it.carbsGrams,
                    fatGrams = it.fatGrams,
                    mealType = runCatching { MealType.valueOf(it.mealType) }.getOrDefault(MealType.SNACK),
                )
            }
            NutritionDailySummary(
                date = start,
                totalCalories = foodEntries.sumOf { it.calories },
                totalProteinGrams = foodEntries.sumOf { it.proteinGrams ?: 0.0 },
                totalCarbsGrams = foodEntries.sumOf { it.carbsGrams ?: 0.0 },
                totalFatGrams = foodEntries.sumOf { it.fatGrams ?: 0.0 },
                totalWaterMilliliters = waterEntities.sumOf { it.milliliters },
                waterGoalMilliliters = waterGoalMilliliters,
                foodEntries = foodEntries,
            )
        }
    }

    /** One-shot read (not a Flow) used by the AI Coach to build a text summary of recent nutrition (see ai/AiCoachRepository). */
    suspend fun caloriesAndWaterForDay(day: LocalDate): Pair<Int, Int> {
        val start = day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val end = day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val food = db.nutritionDao().observeFoodBetween(start, end).first()
        val water = db.nutritionDao().observeWaterBetween(start, end).first()
        return Pair(food.sumOf { it.calories }, water.sumOf { it.milliliters })
    }
}
