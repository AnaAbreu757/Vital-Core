package com.vitalcore.app.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalcore.app.data.healthconnect.HealthConnectAvailability
import com.vitalcore.app.data.healthconnect.HealthConnectManager
import com.vitalcore.app.settings.OnboardingGoal
import com.vitalcore.app.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    val healthConnectManager: HealthConnectManager,
) : ViewModel() {

    fun healthConnectAvailability(): HealthConnectAvailability = healthConnectManager.availability()

    fun completeOnboarding(goal: OnboardingGoal, onDone: () -> Unit) {
        viewModelScope.launch {
            settingsRepository.setOnboardingComplete(goal)
            onDone()
        }
    }
}
