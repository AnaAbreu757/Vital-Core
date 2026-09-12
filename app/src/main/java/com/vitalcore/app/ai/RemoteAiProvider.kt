package com.vitalcore.app.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

/**
 * Calls a user-configured, Anthropic-Messages-API-compatible endpoint
 * (https://api.anthropic.com/v1/messages or a self-hosted proxy with the
 * same shape). Uses plain java.net.HttpURLConnection rather than pulling in
 * a networking library, since this is the only network call in the app and
 * keeping the dependency footprint small matters more than convenience here.
 *
 * The API key lives only in AiSettingsRepository (local DataStore) and is
 * attached per-request — it is never bundled into the app, logged, or sent
 * anywhere except the user's own configured endpoint.
 */
class RemoteAiProvider @Inject constructor(
    private val aiSettingsRepository: AiSettingsRepository,
) : AiProvider {

    override suspend fun ask(systemContext: String, question: String): AiResponse = withContext(Dispatchers.IO) {
        val config = aiSettingsRepository.config
        // Read the latest config synchronously for this single call.
        val current = config.first()
        if (!current.isConfigured) {
            return@withContext AiResponse.Error("AI Coach isn't set up yet. Add a provider endpoint and API key in Settings > AI Coach.")
        }

        try {
            val url = URL(current.endpointUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-api-key", current.apiKey)
                setRequestProperty("anthropic-version", "2023-06-01")
                doOutput = true
                connectTimeout = 15_000
                readTimeout = 30_000
            }

            val body = buildJsonObject {
                put("model", current.model)
                put("max_tokens", 700)
                putJsonArray("messages") {
                    addJsonObject {
                        put("role", "user")
                        put("content", "$systemContext\n\nQuestion: $question")
                    }
                }
            }

            OutputStreamWriter(connection.outputStream).use { it.write(body.toString()) }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errorText = connection.errorStream?.bufferedReader()?.readText() ?: "HTTP $responseCode"
                return@withContext AiResponse.Error("AI Coach request failed: $errorText")
            }

            val responseText = connection.inputStream.bufferedReader().readText()
            val parsed = Json.parseToJsonElement(responseText).jsonObject
            val contentArray = parsed["content"]?.jsonArray ?: JsonArray(emptyList())
            val text = contentArray.joinToString("\n") { block ->
                block.jsonObject["text"]?.jsonPrimitive?.content.orEmpty()
            }.trim()

            if (text.isEmpty()) AiResponse.Error("AI Coach returned an empty response.") else AiResponse.Success(text)
        } catch (t: Throwable) {
            AiResponse.Error("AI Coach couldn't be reached: ${t.message ?: t::class.simpleName}")
        }
    }
}
