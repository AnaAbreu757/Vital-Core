package com.vitalcore.app.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vitalcore.app.data.repositories.StreakRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class PublicProfile(val uid: String, val displayName: String?, val photoUrl: String?)

/**
 * Writes the signed-in user's own public profile + streak stats to Firestore
 * so friends can look them up. Deliberately minimal: name, photo, uid,
 * current/longest streak — never health scores, nutrition, or anything else.
 * See MANUAL_DEPLOY.md "Friends & streaks" for the full privacy tradeoff:
 * this data is readable by any other signed-in VitalCore user, by design,
 * since there's no backend to enforce a stricter "friends-only" rule.
 */
@Singleton
class ProfileRepository @Inject constructor(
    private val streakRepository: StreakRepository,
) {
    private val firestore: FirebaseFirestore? get() = if (FirebaseInit.isConfigured) FirebaseFirestore.getInstance() else null
    private val uid: String? get() = FirebaseAuth.getInstance().currentUser?.uid

    /** Call after sign-in and after each daily score computation to keep the public profile/stats fresh. */
    suspend fun publishProfileAndStats() {
        val store = firestore ?: return
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val streak = streakRepository.currentStreak()

        store.collection("public_profiles").document(user.uid).set(
            mapOf(
                "displayName" to (user.displayName ?: "VitalCore user"),
                "photoUrl" to (user.photoUrl?.toString() ?: ""),
            )
        ).await()

        store.collection("users").document(user.uid).collection("public_stats").document("current").set(
            mapOf(
                "currentStreak" to streak.currentStreak,
                "longestStreak" to streak.longestStreak,
                "lastUpdatedEpochMillis" to System.currentTimeMillis(),
            )
        ).await()
    }

    /** Looks up another user's public profile + streak by their uid ("friend code" — see FriendsRepository). */
    suspend fun lookupPublicProfile(otherUid: String): Pair<PublicProfile, Pair<Int, Int>>? {
        val store = firestore ?: return null
        return try {
            val profileDoc = store.collection("public_profiles").document(otherUid).get().await()
            if (!profileDoc.exists()) return null
            val profile = PublicProfile(
                uid = otherUid,
                displayName = profileDoc.getString("displayName"),
                photoUrl = profileDoc.getString("photoUrl"),
            )
            val statsDoc = store.collection("users").document(otherUid).collection("public_stats").document("current").get().await()
            val streaks = Pair(
                (statsDoc.getLong("currentStreak") ?: 0L).toInt(),
                (statsDoc.getLong("longestStreak") ?: 0L).toInt(),
            )
            profile to streaks
        } catch (t: Throwable) {
            null
        }
    }

    val myUid: String? get() = uid
}
