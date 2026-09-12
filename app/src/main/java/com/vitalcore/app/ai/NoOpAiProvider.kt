package com.vitalcore.app.ai

/** Used whenever no AI provider is configured yet. Keeps the AI Coach screen functional (with a clear call to action) rather than crashing or hanging. */
class NoOpAiProvider : AiProvider {
    override suspend fun ask(systemContext: String, question: String): AiResponse =
        AiResponse.Error("AI Coach isn't set up yet. Add a provider endpoint and API key in Settings > AI Coach.")
}
