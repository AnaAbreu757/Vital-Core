package com.vitalcore.app.ui.screens.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalcore.app.auth.AuthRepository
import com.vitalcore.app.settings.ProfileDetails
import com.vitalcore.app.settings.ProfileSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Bridges the local profile fields (photo/height/weight/birth year, in
 * [ProfileSettingsRepository]) with the Google account photo ([AuthRepository])
 * used as a fallback when no local photo has been picked. Shared by the Home
 * top bar (read-only avatar) and the Settings > Profile card (full editor).
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileSettingsRepository: ProfileSettingsRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val profile: StateFlow<ProfileDetails> = profileSettingsRepository.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileDetails())

    /** Google account photo, used only when no local photo has been picked. */
    val googlePhotoUrl: String? get() = authRepository.currentUser?.photoUrl

    fun setPhotoPath(path: String?) {
        viewModelScope.launch { profileSettingsRepository.setPhotoPath(path) }
    }

    fun setMeasurements(heightCm: Float?, weightKg: Float?, birthYear: Int?) {
        viewModelScope.launch { profileSettingsRepository.setMeasurements(heightCm, weightKg, birthYear) }
    }
}
