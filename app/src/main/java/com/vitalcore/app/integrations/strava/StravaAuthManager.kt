package com.vitalcore.app.integrations.strava

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.vitalcore.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

private const val REDIRECT_URI = "vitalcore://strava-callback"
private const val AUTHORIZE_URL = "https://www.strava.com/oauth/authorize"
private const val TOKEN_URL = "https://www.strava.com/oauth/token"

sealed class StravaAuthResult {
    data class Success(val athleteName: String?) : StravaAuthResult()
    data class Error(val message: String) : StravaAuthResult()
    data object NotConfigured : StravaAuthResult()
}

/**
 * Strava's OAuth2 flow, done properly via Chrome Custom Tabs (the
 * recommended pattern for mobile OAuth — never an embedded WebView, which
 * can't be trusted with credentials). The app's manifest declares an
 * intent-filter for the `vitalcore://strava-callback` redirect so Android
 * routes the browser's redirect straight back into MainActivity.
 *
 * Requires STRAVA_CLIENT_ID/STRAVA_CLIENT_SECRET in local.properties — see
 * MANUAL_DEPLOY.md. Embedding a client secret in a distributed APK is a
 * known simplification for personal/single-user use; a production multi-user
 * release should proxy the token exchange through a small backend instead
 * (documented as a known limitation).
 */
@Singleton
class StravaAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: StravaSettingsRepository,
) {
    val isConfigured: Boolean get() = BuildConfig.STRAVA_CLIENT_ID.isNotBlank() && BuildConfig.STRAVA_CLIENT_SECRET.isNotBlank()

    fun launchAuthorization() {
        val uri = Uri.parse(AUTHORIZE_URL).buildUpon()
            .appendQueryParameter("client_id", BuildConfig.STRAVA_CLIENT_ID)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("approval_prompt", "auto")
            .appendQueryParameter("scope", "read,activity:read_all,profile:read_all")
            .build()
        CustomTabsIntent.Builder().build().launchUrl(context, uri)
    }

    /** Called from MainActivity when the vitalcore://strava-callback deep link is received. */
    suspend fun handleRedirect(uri: Uri): StravaAuthResult {
        if (!isConfigured) return StravaAuthResult.NotConfigured
        val error = uri.getQueryParameter("error")
        if (error != null) return StravaAuthResult.Error("Strava authorization was denied.")
        val code = uri.getQueryParameter("code") ?: return StravaAuthResult.Error("No authorization code returned.")

        return exchangeCodeForTokens(code)
    }

    private suspend fun exchangeCodeForTokens(code: String): StravaAuthResult = withContext(Dispatchers.IO) {
        try {
            val response = postForm(
                TOKEN_URL,
                mapOf(
                    "client_id" to BuildConfig.STRAVA_CLIENT_ID,
                    "client_secret" to BuildConfig.STRAVA_CLIENT_SECRET,
                    "code" to code,
                    "grant_type" to "authorization_code",
                ),
            )
            saveTokenResponse(response)
        } catch (t: Throwable) {
            StravaAuthResult.Error(t.message ?: "Token exchange failed.")
        }
    }

    suspend fun refreshIfNeeded(): StravaAuthResult = withContext(Dispatchers.IO) {
        val tokens = settingsRepository.currentTokens() ?: return@withContext StravaAuthResult.Error("Not connected.")
        if (!tokens.isExpired) return@withContext StravaAuthResult.Success(tokens.athleteName)
        try {
            val response = postForm(
                TOKEN_URL,
                mapOf(
                    "client_id" to BuildConfig.STRAVA_CLIENT_ID,
                    "client_secret" to BuildConfig.STRAVA_CLIENT_SECRET,
                    "refresh_token" to tokens.refreshToken,
                    "grant_type" to "refresh_token",
                ),
            )
            saveTokenResponse(response, fallbackAthleteName = tokens.athleteName)
        } catch (t: Throwable) {
            StravaAuthResult.Error(t.message ?: "Token refresh failed.")
        }
    }

    private suspend fun saveTokenResponse(responseText: String, fallbackAthleteName: String? = null): StravaAuthResult {
        val json = Json.parseToJsonElement(responseText).jsonObject
        val accessToken = json["access_token"]?.jsonPrimitive?.content
            ?: return StravaAuthResult.Error("No access token in response.")
        val refreshToken = json["refresh_token"]?.jsonPrimitive?.content
            ?: return StravaAuthResult.Error("No refresh token in response.")
        val expiresAt = json["expires_at"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
        val athleteName = json["athlete"]?.jsonObject?.let { athlete ->
            val first = athlete["firstname"]?.jsonPrimitive?.content.orEmpty()
            val last = athlete["lastname"]?.jsonPrimitive?.content.orEmpty()
            "$first $last".trim().ifBlank { null }
        } ?: fallbackAthleteName

        settingsRepository.saveTokens(accessToken, refreshToken, expiresAt, athleteName)
        return StravaAuthResult.Success(athleteName)
    }

    private fun postForm(urlString: String, params: Map<String, String>): String {
        val url = URL(urlString)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 20_000
        }
        val body = params.entries.joinToString("&") { (k, v) -> "${Uri.encode(k)}=${Uri.encode(v)}" }
        OutputStreamWriter(connection.outputStream).use { it.write(body) }

        val code = connection.responseCode
        return if (code in 200..299) {
            connection.inputStream.bufferedReader().readText()
        } else {
            val err = connection.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"
            throw RuntimeException(err)
        }
    }
}
