package com.vitalcore.app.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vitalcore.app.domain.calculations.RecoveryWeights
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "vitalcore_settings")

enum class OnboardingGoal { IMPROVE_FITNESS, IMPROVE_SLEEP, BUILD_MUSCLE, LOSE_WEIGHT, GENERAL_HEALTH, PERFORMANCE }

/**
 * User preferences: units, notification toggles, Recovery score component
 * weights, onboarding state. Backed by Jetpack DataStore (not Room — this is
 * small, key-value, app-level preference data, not health records).
 */
@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val GOAL = stringPreferencesKey("onboarding_goal")
        val NOTIF_MORNING_SUMMARY = booleanPreferencesKey("notif_morning_summary")
        val NOTIF_SLEEP_REMINDER = booleanPreferencesKey("notif_sleep_reminder")
        val NOTIF_HIGH_STRAIN = booleanPreferencesKey("notif_high_strain")
        val NOTIF_LOW_RECOVERY = booleanPreferencesKey("notif_low_recovery")
        val WEIGHT_HRV = doublePreferencesKey("weight_hrv")
        val WEIGHT_RHR = doublePreferencesKey("weight_rhr")
        val WEIGHT_SLEEP = doublePreferencesKey("weight_sleep")
    }

    val onboardingComplete: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDING_COMPLETE] ?: false }

    suspend fun setOnboardingComplete(goal: OnboardingGoal) {
        context.dataStore.edit {
            it[Keys.ONBOARDING_COMPLETE] = true
            it[Keys.GOAL] = goal.name
        }
    }

    val notificationSettings: Flow<NotificationSettings> = context.dataStore.data.map {
        NotificationSettings(
            morningSummary = it[Keys.NOTIF_MORNING_SUMMARY] ?: true,
            sleepReminder = it[Keys.NOTIF_SLEEP_REMINDER] ?: false,
            highStrain = it[Keys.NOTIF_HIGH_STRAIN] ?: true,
            lowRecovery = it[Keys.NOTIF_LOW_RECOVERY] ?: true,
        )
    }

    suspend fun setMorningSummary(enabled: Boolean) = context.dataStore.edit { it[Keys.NOTIF_MORNING_SUMMARY] = enabled }
    suspend fun setSleepReminder(enabled: Boolean) = context.dataStore.edit { it[Keys.NOTIF_SLEEP_REMINDER] = enabled }
    suspend fun setHighStrain(enabled: Boolean) = context.dataStore.edit { it[Keys.NOTIF_HIGH_STRAIN] = enabled }
    suspend fun setLowRecovery(enabled: Boolean) = context.dataStore.edit { it[Keys.NOTIF_LOW_RECOVERY] = enabled }

    val recoveryWeights: Flow<RecoveryWeights> = context.dataStore.data.map {
        val hrv = it[Keys.WEIGHT_HRV] ?: 0.35
        val rhr = it[Keys.WEIGHT_RHR] ?: 0.20
        val sleep = it[Keys.WEIGHT_SLEEP] ?: 0.20
        val remaining = (1.0 - hrv - rhr - sleep).coerceAtLeast(0.0)
        RecoveryWeights(
            hrv = hrv, restingHeartRate = rhr, sleep = sleep,
            respiratoryRate = remaining * 0.4, spo2 = remaining * 0.2,
            temperature = remaining * 0.2, trainingLoad = remaining * 0.2,
        )
    }

    suspend fun clearAll() = context.dataStore.edit { it.clear() }
}

data class NotificationSettings(
    val morningSummary: Boolean,
    val sleepReminder: Boolean,
    val highStrain: Boolean,
    val lowRecovery: Boolean,
)
