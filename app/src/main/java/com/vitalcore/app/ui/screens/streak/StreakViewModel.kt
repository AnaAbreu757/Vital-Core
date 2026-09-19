package com.vitalcore.app.ui.screens.streak

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalcore.app.auth.ProfileRepository
import com.vitalcore.app.data.repositories.StreakRepository
import com.vitalcore.app.domain.calculations.StreakResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StreakViewModel @Inject constructor(
    private val streakRepository: StreakRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _streak = MutableStateFlow<StreakResult?>(null)
    val streak: StateFlow<StreakResult?> = _streak.asStateFlow()

    init {
        viewModelScope.launch {
            val result = streakRepository.currentStreak()
            _streak.value = result
            profileRepository.publishProfileAndStats()
        }
    }
}
