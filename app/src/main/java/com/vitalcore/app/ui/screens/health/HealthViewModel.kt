package com.vitalcore.app.ui.screens.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalcore.app.data.repositories.HealthDataRepository
import com.vitalcore.app.domain.calculations.DataQualityCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HealthUiState(
    val loading: Boolean = true,
    val hrvMillis: Double? = null,
    val restingHeartRateBpm: Int? = null,
    val respiratoryRateBpm: Double? = null,
    val spo2Percentage: Double? = null,
    val dataQualityPercent: Int = 0,
    val missingSignals: List<String> = emptyList(),
)

@HiltViewModel
class HealthViewModel @Inject constructor(
    private val healthDataRepository: HealthDataRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HealthUiState())
    val uiState: StateFlow<HealthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val snapshot = healthDataRepository.snapshotForDay(LocalDate.now())
            val quality = DataQualityCalculator.evaluate(snapshot)
            _uiState.value = HealthUiState(
                loading = false,
                hrvMillis = snapshot.avgHrvMillis,
                restingHeartRateBpm = snapshot.restingHeartRateBpm,
                respiratoryRateBpm = snapshot.respiratoryRateBpm,
                spo2Percentage = snapshot.avgSpo2Percentage,
                dataQualityPercent = quality.percent,
                missingSignals = quality.missing,
            )
        }
    }
}
