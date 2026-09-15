package com.vitalcore.app.ui.navigation

import androidx.lifecycle.ViewModel
import com.vitalcore.app.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Exposes whether onboarding has been completed, so [VitalCoreNavGraph] can decide the start screen. */
@HiltViewModel
class NavigationGateViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {
    val onboardingComplete: Flow<Boolean> = settingsRepository.onboardingComplete
}
