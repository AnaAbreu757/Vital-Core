package com.vitalcore.app.integrations.strava

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.stravaDataStore by preferencesDataStore(name = "vitalcore_strava")

data class StravaTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochSeconds: Long,
    val athleteName: String?,
) {
    val isExpired: Boolean get() = System.currentTimeMillis() / 1000 >= expiresAtEpochSeconds - 60
}

/** Stores Strava OAuth tokens locally only — see PRIVACY.md. Nothing is sent anywhere except Strava's own API. */
@Singleton
class StravaSettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val ACCESS_TOKEN = stringPreferencesKey("strava_access_token")
        val REFRESH_TOKEN = stringPreferencesKey("strava_refresh_token")
        val EXPIRES_AT = longPreferencesKey("strava_expires_at")
        val ATHLETE_NAME = stringPreferencesKey("strava_athlete_name")
        val LAST_SYNC_EPOCH_MILLIS = longPreferencesKey("strava_last_sync")
    }

    val tokens: Flow<StravaTokens?> = context.stravaDataStore.data.map {
        val access = it[Keys.ACCESS_TOKEN] ?: return@map null
        val refresh = it[Keys.REFRESH_TOKEN] ?: return@map null
        StravaTokens(access, refresh, it[Keys.EXPIRES_AT] ?: 0L, it[Keys.ATHLETE_NAME])
    }

    suspend fun currentTokens(): StravaTokens? = tokens.first()

    suspend fun saveTokens(accessToken: String, refreshToken: String, expiresAtEpochSeconds: Long, athleteName: String?) {
        context.stravaDataStore.edit {
            it[Keys.ACCESS_TOKEN] = accessToken
            it[Keys.REFRESH_TOKEN] = refreshToken
            it[Keys.EXPIRES_AT] = expiresAtEpochSeconds
            athleteName?.let { name -> it[Keys.ATHLETE_NAME] = name }
        }
    }

    suspend fun setLastSync(epochMillis: Long) = context.stravaDataStore.edit { it[Keys.LAST_SYNC_EPOCH_MILLIS] = epochMillis }
    val lastSync: Flow<Long?> = context.stravaDataStore.data.map { it[Keys.LAST_SYNC_EPOCH_MILLIS] }

    suspend fun disconnect() = context.stravaDataStore.edit { it.clear() }
}
