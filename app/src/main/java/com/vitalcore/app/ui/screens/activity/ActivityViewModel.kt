package com.vitalcore.app.ui.screens.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalcore.app.data.repositories.HealthDataRepository
import com.vitalcore.app.data.repositories.ScoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ActivityTodayState(
    val steps: Long? = null,
    val calories: Double? = null,
    val exerciseCount: Int = 0,
)

@HiltViewModel
class ActivityViewModel @Inject constructor(
    scoreRepository: ScoreRepository,
    private val healthDataRepository: HealthDataRepository,
) : ViewModel() {
    private val today = LocalDate.now()

    val last90DaysStrain: StateFlow<List<Float>> = scoreRepository
        .observeStrain(today.minusDays(89), today)
        .map { list -> list.map { it.score.toFloat() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _todayState = MutableStateFlow(ActivityTodayState())
    val todayState: StateFlow<ActivityTodayState> = _todayState.asStateFlow()

    init {
        viewModelScope.launch {
            val snapshot = healthDataRepository.snapshotForDay(today)
            _todayState.value = ActivityTodayState(
                steps = snapshot.activity?.steps,
                calories = snapshot.activity?.totalCalories,
                exerciseCount = snapshot.exerciseSessions.size,
            )
        }
    }
}
