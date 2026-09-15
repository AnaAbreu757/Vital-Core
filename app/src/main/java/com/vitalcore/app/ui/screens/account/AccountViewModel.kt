package com.vitalcore.app.ui.screens.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalcore.app.auth.AuthRepository
import com.vitalcore.app.auth.CloudSyncRepository
import com.vitalcore.app.auth.ProfileRepository
import com.vitalcore.app.auth.FirebaseInit
import com.vitalcore.app.auth.SignInResult
import com.vitalcore.app.auth.SyncResult
import com.vitalcore.app.auth.VitalCoreUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountUiState(
    val user: VitalCoreUser? = null,
    val firebaseConfigured: Boolean = FirebaseInit.isConfigured,
    val busy: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val cloudSyncRepository: CloudSyncRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    val currentUser: StateFlow<VitalCoreUser?> = authRepository.observeAuthState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), authRepository.currentUser)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun signIn() {
        _busy.value = true
        viewModelScope.launch {
            when (val result = authRepository.signInWithGoogle()) {
                is SignInResult.Success -> {
                    _message.value = "Signed in as ${result.user.email ?: result.user.displayName}"
                    runCatching { profileRepository.publishProfileAndStats() }
                }
                is SignInResult.Error -> _message.value = result.message
                SignInResult.NotConfigured -> _message.value =
                    "Google Sign-In isn't configured yet. Add GOOGLE_WEB_CLIENT_ID to local.properties — see MANUAL_DEPLOY.md."
            }
            _busy.value = false
        }
    }

    fun signOut() {
        authRepository.signOut()
        _message.value = "Signed out."
    }

    fun backupNow() {
        _busy.value = true
        viewModelScope.launch {
            _message.value = when (val result = cloudSyncRepository.backupNow()) {
                SyncResult.Success -> "Backup complete."
                SyncResult.NotSignedIn -> "Sign in first to back up to the cloud."
                SyncResult.NotConfigured -> "Firebase isn't configured yet — see MANUAL_DEPLOY.md."
                is SyncResult.Error -> "Backup failed: ${result.message}"
            }
            _busy.value = false
        }
    }
}
