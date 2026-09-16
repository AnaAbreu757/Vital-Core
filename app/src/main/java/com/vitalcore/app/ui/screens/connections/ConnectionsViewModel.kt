package com.vitalcore.app.ui.screens.connections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalcore.app.integrations.strava.StravaAuthManager
import com.vitalcore.app.integrations.strava.StravaSettingsRepository
import com.vitalcore.app.integrations.strava.StravaSyncRepository
import com.vitalcore.app.integrations.strava.StravaSyncResult
import com.vitalcore.app.integrations.strava.StravaTokens
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConnectionsViewModel @Inject constructor(
    val stravaAuthManager: StravaAuthManager,
    private val stravaSettingsRepository: StravaSettingsRepository,
    private val stravaSyncRepository: StravaSyncRepository,
) : ViewModel() {

    val stravaTokens: StateFlow<StravaTokens?> = stravaSettingsRepository.tokens
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    fun connectStrava() {
        if (!stravaAuthManager.isConfigured) {
            _message.value = "Strava isn't configured yet. Add STRAVA_CLIENT_ID/SECRET to local.properties — see MANUAL_DEPLOY.md."
            return
        }
        stravaAuthManager.launchAuthorization()
    }

    fun disconnectStrava() = viewModelScope.launch { stravaSettingsRepository.disconnect() }

    fun syncStrava() {
        _syncing.value = true
        viewModelScope.launch {
            _message.value = when (val result = stravaSyncRepository.syncRecentActivities()) {
                is StravaSyncResult.Success -> "Imported ${result.importedCount} Strava activities."
                StravaSyncResult.NotConnected -> "Connect Strava first."
                is StravaSyncResult.Error -> "Strava sync failed: ${result.message}"
            }
            _syncing.value = false
        }
    }
}
