package com.vitalcore.app.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.profileDataStore by preferencesDataStore(name = "profile_settings")

data class ProfileDetails(
    val photoPath: String? = null,
    val heightCm: Float? = null,
    val weightKg: Float? = null,
    val birthYear: Int? = null,
) {
    /** Simple, well-known age-based estimate (220 − age); used as a fallback
     * wherever a real measured max heart rate isn't available. Null until a
     * birth year is set. */
    val estimatedMaxHeartRate: Int?
        get() = birthYear?.let { by ->
            val age = java.time.Year.now().value - by
            (220 - age).coerceIn(120, 220)
        }
}

/**
 * Local, on-device profile fields: a picked photo (stored as a file under
 * the app's private storage, not a content:// Uri, so it survives past the
 * grant lifetime of a one-off picker Uri) plus height/weight/birth year.
 * Entirely separate from the Google account photo (AuthRepository) — this
 * is a user-chosen override shown in place of it when set.
 */
@Singleton
class ProfileSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val PHOTO_PATH = stringPreferencesKey("photo_path")
        val HEIGHT_CM = floatPreferencesKey("height_cm")
        val WEIGHT_KG = floatPreferencesKey("weight_kg")
        val BIRTH_YEAR = intPreferencesKey("birth_year")
    }

    val profile: Flow<ProfileDetails> = context.profileDataStore.data.map { prefs ->
        ProfileDetails(
            photoPath = prefs[Keys.PHOTO_PATH],
            heightCm = prefs[Keys.HEIGHT_CM],
            weightKg = prefs[Keys.WEIGHT_KG],
            birthYear = prefs[Keys.BIRTH_YEAR],
        )
    }

    suspend fun setPhotoPath(path: String?) {
        context.profileDataStore.edit { prefs ->
            if (path == null) prefs.remove(Keys.PHOTO_PATH) else prefs[Keys.PHOTO_PATH] = path
        }
    }

    suspend fun setMeasurements(heightCm: Float?, weightKg: Float?, birthYear: Int?) {
        context.profileDataStore.edit { prefs ->
            if (heightCm == null) prefs.remove(Keys.HEIGHT_CM) else prefs[Keys.HEIGHT_CM] = heightCm
            if (weightKg == null) prefs.remove(Keys.WEIGHT_KG) else prefs[Keys.WEIGHT_KG] = weightKg
            if (birthYear == null) prefs.remove(Keys.BIRTH_YEAR) else prefs[Keys.BIRTH_YEAR] = birthYear
        }
    }
}
