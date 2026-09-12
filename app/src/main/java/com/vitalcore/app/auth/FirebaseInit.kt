package com.vitalcore.app.auth

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.vitalcore.app.BuildConfig

/**
 * Initializes Firebase programmatically from BuildConfig fields (sourced
 * from local.properties) instead of requiring the Google Services Gradle
 * plugin + a google-services.json resource file. This keeps the project
 * buildable without any Firebase setup, and keeps all credentials in one
 * place (local.properties) alongside the AI Coach / Strava pattern used
 * elsewhere in the app.
 *
 * No-ops (returns false) when FIREBASE_API_KEY is blank, so callers can
 * check [isConfigured] before touching Firebase Auth/Firestore.
 */
object FirebaseInit {
    var isConfigured: Boolean = false
        private set

    fun initialize(context: Context) {
        if (BuildConfig.FIREBASE_API_KEY.isBlank() || BuildConfig.FIREBASE_APP_ID.isBlank()) {
            isConfigured = false
            return
        }
        val options = FirebaseOptions.Builder()
            .setApiKey(BuildConfig.FIREBASE_API_KEY)
            .setApplicationId(BuildConfig.FIREBASE_APP_ID)
            .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
            .setStorageBucket(BuildConfig.FIREBASE_STORAGE_BUCKET)
            .build()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context, options)
        }
        isConfigured = true
    }
}
