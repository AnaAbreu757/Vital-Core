package com.vitalcore.app.domain.model

import java.time.Instant

enum class MealType { BREAKFAST, LUNCH, DINNER, SNACK }

data class FoodEntry(
    val id: Long = 0,
    val time: Instant,
    val name: String,
    val calories: Int,
    val proteinGrams: Double? = null,
    val carbsGrams: Double? = null,
    val fatGrams: Double? = null,
    val mealType: MealType = MealType.SNACK,
)

data class WaterEntry(
    val id: Long = 0,
    val time: Instant,
    val milliliters: Int,
)

data class NutritionDailySummary(
    val date: Instant,
    val totalCalories: Int,
    val totalProteinGrams: Double,
    val totalCarbsGrams: Double,
    val totalFatGrams: Double,
    val totalWaterMilliliters: Int,
    val waterGoalMilliliters: Int,
    val foodEntries: List<FoodEntry>,
)
