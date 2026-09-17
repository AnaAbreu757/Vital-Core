package com.vitalcore.app.ui.screens.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalcore.app.data.repositories.NutritionRepository
import com.vitalcore.app.domain.model.MealType
import com.vitalcore.app.domain.model.NutritionDailySummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val nutritionRepository: NutritionRepository,
) : ViewModel() {

    val today: StateFlow<NutritionDailySummary> = nutritionRepository
        .observeDailySummary(LocalDate.now())
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            NutritionDailySummary(Instant.now(), 0, 0.0, 0.0, 0.0, 0, 2000, emptyList()),
        )

    fun addWater(milliliters: Int) = viewModelScope.launch {
        nutritionRepository.logWater(Instant.now(), milliliters)
    }

    fun addFood(
        name: String,
        calories: Int,
        proteinGrams: Double?,
        carbsGrams: Double?,
        fatGrams: Double?,
        mealType: MealType,
    ) = viewModelScope.launch {
        nutritionRepository.logFood(Instant.now(), name, calories, proteinGrams, carbsGrams, fatGrams, mealType)
    }

    fun deleteFood(id: Long) = viewModelScope.launch { nutritionRepository.deleteFood(id) }
    fun deleteWater(id: Long) = viewModelScope.launch { nutritionRepository.deleteWater(id) }
}
