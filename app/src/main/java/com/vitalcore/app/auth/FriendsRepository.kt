package com.vitalcore.app.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class Friend(val uid: String, val displayName: String?, val photoUrl: String?, val currentStreak: Int, val longestStreak: Int)

sealed class AddFriendResult {
    data class Success(val friend: Friend) : AddFriendResult()
    data object NotFound : AddFriendResult()
    data object NotConfigured : AddFriendResult()
    data class Error(val message: String) : AddFriendResult()
}

/**
 * A one-directional "follow" model, not mutual friendship — see
 * MANUAL_DEPLOY.md "Friends & streaks" for exactly why: a correct,
 * server-enforced *mutual* friend system needs a backend (typically a Cloud
 * Function) to safely mirror writes across two users' data; without one,
 * this is the honest, secure alternative. Each person shares their own uid
 * ("friend code", visible on their own Profile screen) and anyone who adds
 * it can see that person's public streak — nothing else.
 */
@Singleton
class FriendsRepository @Inject constructor(
    private val profileRepository: ProfileRepository,
) {
    private val firestore: FirebaseFirestore? get() = if (FirebaseInit.isConfigured) FirebaseFirestore.getInstance() else null
    private val uid: String? get() = FirebaseAuth.getInstance().currentUser?.uid

    suspend fun addFriend(friendCode: String): AddFriendResult {
        val store = firestore ?: return AddFriendResult.NotConfigured
        val myUid = uid ?: return AddFriendResult.NotConfigured
        val trimmed = friendCode.trim()
        if (trimmed.isBlank() || trimmed == myUid) return AddFriendResult.NotFound

        val lookup = profileRepository.lookupPublicProfile(trimmed) ?: return AddFriendResult.NotFound
        val (profile, streaks) = lookup

        return try {
            store.collection("users").document(myUid).collection("friends").document(trimmed).set(
                mapOf("displayName" to profile.displayName, "photoUrl" to profile.photoUrl)
            ).await()
            AddFriendResult.Success(Friend(profile.uid, profile.displayName, profile.photoUrl, streaks.first, streaks.second))
        } catch (t: Throwable) {
            AddFriendResult.Error(t.message ?: "Couldn't add friend.")
        }
    }

    suspend fun removeFriend(friendUid: String) {
        val store = firestore ?: return
        val myUid = uid ?: return
        store.collection("users").document(myUid).collection("friends").document(friendUid).delete().await()
    }

    /** Fetches the friends list with each friend's *current* streak (re-read live, not the cached name-only doc). */
    suspend fun listFriends(): List<Friend> {
        val store = firestore ?: return emptyList()
        val myUid = uid ?: return emptyList()
        return try {
            val snapshot = store.collection("users").document(myUid).collection("friends").get().await()
            snapshot.documents.mapNotNull { doc ->
                val friendUid = doc.id
                val lookup = profileRepository.lookupPublicProfile(friendUid)
                val cachedName = doc.getString("displayName")
                val cachedPhoto = doc.getString("photoUrl")
                if (lookup != null) {
                    val (profile, streaks) = lookup
                    Friend(friendUid, profile.displayName ?: cachedName, profile.photoUrl ?: cachedPhoto, streaks.first, streaks.second)
                } else {
                    Friend(friendUid, cachedName, cachedPhoto, 0, 0)
                }
            }
        } catch (t: Throwable) {
            emptyList()
        }
    }
}
