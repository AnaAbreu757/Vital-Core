package com.vitalcore.app.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.vitalcore.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

data class VitalCoreUser(val uid: String, val displayName: String?, val email: String?, val photoUrl: String?)

sealed class SignInResult {
    data class Success(val user: VitalCoreUser) : SignInResult()
    data class Error(val message: String) : SignInResult()
    /** No GOOGLE_WEB_CLIENT_ID configured in local.properties — see MANUAL_DEPLOY.md. */
    data object NotConfigured : SignInResult()
}

/**
 * Google Sign-In via Credential Manager (the current, non-deprecated API —
 * GoogleSignInClient is legacy), federated into Firebase Auth. Requires
 * FIREBASE_* and GOOGLE_WEB_CLIENT_ID in local.properties; see FirebaseInit
 * and MANUAL_DEPLOY.md. Fully inert (returns NotConfigured) until then —
 * the rest of the app works with no account at all.
 */
@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val credentialManager by lazy { CredentialManager.create(context) }

    val currentUser: VitalCoreUser?
        get() = auth.currentUser?.let {
            VitalCoreUser(it.uid, it.displayName, it.email, it.photoUrl?.toString())
        }

    /** Emits the current user (or null) immediately and on every sign-in/sign-out. */
    fun observeAuthState(): Flow<VitalCoreUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.let { VitalCoreUser(it.uid, it.displayName, it.email, it.photoUrl?.toString()) })
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signInWithGoogle(): SignInResult {
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) return SignInResult.NotConfigured

        val nonce = generateNonce()
        val option = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setNonce(nonce)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

        return try {
            val response = credentialManager.getCredential(context, request)
            val credential = response.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val authResult = auth.signInWithCredential(firebaseCredential).await()
                val user = authResult.user ?: return SignInResult.Error("Sign-in succeeded but no user was returned.")
                SignInResult.Success(VitalCoreUser(user.uid, user.displayName, user.email, user.photoUrl?.toString()))
            } else {
                SignInResult.Error("Unexpected credential type returned.")
            }
        } catch (e: GoogleIdTokenParsingException) {
            SignInResult.Error("Couldn't parse Google ID token: ${e.message}")
        } catch (e: GetCredentialException) {
            SignInResult.Error(e.message ?: "Sign-in was cancelled or failed.")
        } catch (t: Throwable) {
            SignInResult.Error(t.message ?: "Unknown sign-in error.")
        }
    }

    fun signOut() {
        auth.signOut()
    }

    private fun generateNonce(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
