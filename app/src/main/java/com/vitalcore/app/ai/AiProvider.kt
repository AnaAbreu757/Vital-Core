package com.vitalcore.app.ai

/**
 * Abstraction over "whatever AI backend the user has configured." VitalCore
 * never ships an API key inside the app binary — the user supplies their own
 * endpoint + key in Settings > AI Coach, stored locally via DataStore
 * (see AiSettingsRepository). The calculation engine (Recovery/Sleep/Strain)
 * works fully with no AiProvider configured; this is purely additive.
 */
interface AiProvider {
    suspend fun ask(systemContext: String, question: String): AiResponse
}

sealed class AiResponse {
    data class Success(val text: String) : AiResponse()
    data class Error(val message: String) : AiResponse()
}
