package com.vitalcore.app.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.vitalcore.app.data.database.VitalCoreDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

sealed class SyncResult {
    data object Success : SyncResult()
    data object NotSignedIn : SyncResult()
    data object NotConfigured : SyncResult()
    data class Error(val message: String) : SyncResult()
}

/**
 * Backs up Recovery/Sleep/Strain scores and nutrition to Firestore under
 * `users/{uid}/...`, and can restore them on a new device after signing in.
 * This is a backup/sync layer only — the local Room database on-device
 * remains the source of truth for every calculation; nothing here feeds
 * back into the scoring engine directly.
 *
 * Every document is scoped under the signed-in user's own uid, and
 * Firestore security rules (see MANUAL_DEPLOY.md) must restrict each user to
 * their own subtree — this class does not enforce that itself, the backend
 * rules do.
 */
@Singleton
class CloudSyncRepository @Inject constructor(
    private val db: VitalCoreDatabase,
) {
    private val firestore: FirebaseFirestore? get() = if (FirebaseInit.isConfigured) FirebaseFirestore.getInstance() else null
    private val uid: String? get() = FirebaseAuth.getInstance().currentUser?.uid

    suspend fun backupNow(): SyncResult = withContext(Dispatchers.IO) {
        val store = firestore ?: return@withContext SyncResult.NotConfigured
        val userId = uid ?: return@withContext SyncResult.NotSignedIn

        try {
            val recovery = db.scoresDao().observeRecoveryRange(-365_000, 365_000)
            val sleep = db.scoresDao().observeSleepRange(-365_000, 365_000)
            val strain = db.scoresDao().observeStrainRange(-365_000, 365_000)

            val recoveryData = recovery.first().associate {
                it.dateEpochDay.toString() to mapOf("score" to it.score, "confidence" to it.confidence)
            }
            val sleepData = sleep.first().associate {
                it.dateEpochDay.toString() to mapOf("score" to it.score, "durationMinutes" to it.durationMinutes)
            }
            val strainData = strain.first().associate {
                it.dateEpochDay.toString() to mapOf("score" to it.score)
            }

            val userDoc = store.collection("users").document(userId)
            userDoc.set(mapOf("lastBackupEpochDay" to LocalDate.now().toEpochDay()), SetOptions.merge()).await()
            userDoc.collection("scores").document("recovery").set(recoveryData).await()
            userDoc.collection("scores").document("sleep").set(sleepData).await()
            userDoc.collection("scores").document("strain").set(strainData).await()

            SyncResult.Success
        } catch (t: Throwable) {
            SyncResult.Error(t.message ?: "Backup failed.")
        }
    }

    /** Pulls the last cloud backup down (used after signing in on a new device). Local data already present is not overwritten by this MVP — see MANUAL_DEPLOY.md "Known limitations". */
    suspend fun restoreLatestBackupTimestamp(): Long? = withContext(Dispatchers.IO) {
        val store = firestore ?: return@withContext null
        val userId = uid ?: return@withContext null
        try {
            val doc = store.collection("users").document(userId).get().await()
            doc.getLong("lastBackupEpochDay")
        } catch (t: Throwable) {
            null
        }
    }
}
