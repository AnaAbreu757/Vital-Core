package com.vitalcore.app.ui.screens.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalcore.app.data.repositories.HealthDataRepository
import com.vitalcore.app.data.repositories.ScoreRepository
import com.vitalcore.app.domain.calculations.BiologicalAgeCalculator
import com.vitalcore.app.domain.calculations.BiologicalAgeResult
import com.vitalcore.app.settings.ProfileSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Year
import javax.inject.Inject

data class CoachUiState(
    val loading: Boolean = true,
    val biologicalAge: BiologicalAgeResult? = null,
)

@HiltViewModel
class CoachViewModel @Inject constructor(
    private val profileSettingsRepository: ProfileSettingsRepository,
    private val scoreRepository: ScoreRepository,
    private val healthDataRepository: HealthDataRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CoachUiState())
    val state: StateFlow<CoachUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val profile = profileSettingsRepository.profile.first()
            val chronologicalAge = profile.birthYear?.let { Year.now().value - it }

            val today = LocalDate.now()
            val start = today.minusDays(29)

            val recoveryScores = scoreRepository.observeRecovery(start, today).first()
            val avgRecovery = recoveryScores.map { it.score }.takeIf { it.isNotEmpty() }?.average()

            val snapshots = healthDataRepository.snapshotsForRange(start, today)
            val sleepHours = snapshots.mapNotNull { it.sleep?.durationMinutes?.let { m -> m / 60.0 } }
            val avgSleepHours = sleepHours.takeIf { it.isNotEmpty() }?.average()
            val steps = snapshots.mapNotNull { it.activity?.steps?.toDouble() }
            val avgSteps = steps.takeIf { it.isNotEmpty() }?.average()

            _state.value = CoachUiState(
                loading = false,
                biologicalAge = BiologicalAgeCalculator.calculate(chronologicalAge, avgRecovery, avgSleepHours, avgSteps),
            )
        }
    }
}
