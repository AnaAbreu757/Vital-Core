package com.vitalcore.app.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalcore.app.ai.AiConfig
import com.vitalcore.app.ai.AiSettingsRepository
import com.vitalcore.app.notifications.NotificationScheduler
import com.vitalcore.app.settings.NotificationSettings
import com.vitalcore.app.settings.PrivacyExportManager
import com.vitalcore.app.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val notifications: NotificationSettings = NotificationSettings(true, false, true, true),
    val lastExportPath: String? = null,
    val dataDeleted: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val privacyExportManager: PrivacyExportManager,
    private val notificationScheduler: NotificationScheduler,
    private val aiSettingsRepository: AiSettingsRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    val notificationSettings: StateFlow<NotificationSettings> = settingsRepository.notificationSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NotificationSettings(true, false, true, true))

    val aiConfig: StateFlow<AiConfig> = aiSettingsRepository.config
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AiConfig(false, "", "", "claude-sonnet-4-6"))

    fun setMorningSummary(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setMorningSummary(enabled)
        notificationScheduler.scheduleMorningSummary(enabled)
    }

    fun setSleepReminder(enabled: Boolean) = viewModelScope.launch { settingsRepository.setSleepReminder(enabled) }
    fun setHighStrain(enabled: Boolean) = viewModelScope.launch { settingsRepository.setHighStrain(enabled) }
    fun setLowRecovery(enabled: Boolean) = viewModelScope.launch { settingsRepository.setLowRecovery(enabled) }

    fun saveAiConfig(enabled: Boolean, endpointUrl: String, apiKey: String, model: String) = viewModelScope.launch {
        aiSettingsRepository.save(enabled, endpointUrl, apiKey, model)
    }

    fun clearAiConfig() = viewModelScope.launch { aiSettingsRepository.clear() }

    fun exportJson(onDone: (File) -> Unit) = viewModelScope.launch {
        val json = privacyExportManager.exportAsJson()
        val file = File(appContext.getExternalFilesDir(null), "vitalcore-export.json")
        privacyExportManager.writeExportFile(json, file)
        onDone(file)
    }

    fun exportCsv(onDone: (File) -> Unit) = viewModelScope.launch {
        val csv = privacyExportManager.exportAsCsv()
        val file = File(appContext.getExternalFilesDir(null), "vitalcore-export.csv")
        privacyExportManager.writeExportFile(csv, file)
        onDone(file)
    }

    fun deleteAllData(onDone: () -> Unit) = viewModelScope.launch {
        privacyExportManager.deleteAllData()
        aiSettingsRepository.clear()
        notificationScheduler.cancelAll()
        onDone()
    }
}
