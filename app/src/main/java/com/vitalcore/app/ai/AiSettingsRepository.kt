package com.vitalcore.app.ai

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.aiDataStore by preferencesDataStore(name = "vitalcore_ai_settings")

data class AiConfig(
    val enabled: Boolean,
    val endpointUrl: String,
    val apiKey: String,
    val model: String,
) {
    val isConfigured: Boolean get() = enabled && endpointUrl.isNotBlank() && apiKey.isNotBlank()
}

/**
 * Stores the user's own AI provider configuration — endpoint URL, API key,
 * model name — entirely locally via DataStore. This is deliberately a
 * separate DataStore file from SettingsRepository's so a data export/import
 * of general app settings never accidentally bundles a live API key.
 */
@Singleton
class AiSettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val ENABLED = booleanPreferencesKey("ai_enabled")
        val ENDPOINT = stringPreferencesKey("ai_endpoint_url")
        val API_KEY = stringPreferencesKey("ai_api_key")
        val MODEL = stringPreferencesKey("ai_model")
    }

    val config: Flow<AiConfig> = context.aiDataStore.data.map {
        AiConfig(
            enabled = it[Keys.ENABLED] ?: false,
            endpointUrl = it[Keys.ENDPOINT] ?: "",
            apiKey = it[Keys.API_KEY] ?: "",
            model = it[Keys.MODEL] ?: "claude-sonnet-4-6",
        )
    }

    suspend fun save(enabled: Boolean, endpointUrl: String, apiKey: String, model: String) {
        context.aiDataStore.edit {
            it[Keys.ENABLED] = enabled
            it[Keys.ENDPOINT] = endpointUrl
            it[Keys.API_KEY] = apiKey
            it[Keys.MODEL] = model
        }
    }

    suspend fun clear() = context.aiDataStore.edit { it.clear() }
}
