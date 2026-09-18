package com.vitalcore.app.ui.screens.recovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalcore.app.data.repositories.ScoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class RecoveryViewModel @Inject constructor(
    scoreRepository: ScoreRepository,
) : ViewModel() {

    private val today = LocalDate.now()

    val last90DaysScores: StateFlow<List<Float>> = scoreRepository
        .observeRecovery(today.minusDays(89), today)
        .map { list -> list.map { it.score.toFloat() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
