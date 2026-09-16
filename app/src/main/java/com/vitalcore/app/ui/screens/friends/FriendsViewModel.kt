package com.vitalcore.app.ui.screens.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalcore.app.auth.AddFriendResult
import com.vitalcore.app.auth.Friend
import com.vitalcore.app.auth.FriendsRepository
import com.vitalcore.app.auth.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FriendsUiState(
    val myUid: String? = null,
    val friends: List<Friend> = emptyList(),
    val loading: Boolean = true,
    val message: String? = null,
)

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val friendsRepository: FriendsRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, myUid = profileRepository.myUid)
            val friends = friendsRepository.listFriends()
            _uiState.value = _uiState.value.copy(loading = false, friends = friends, myUid = profileRepository.myUid)
        }
    }

    fun addFriend(code: String) {
        viewModelScope.launch {
            when (val result = friendsRepository.addFriend(code)) {
                is AddFriendResult.Success -> {
                    _uiState.value = _uiState.value.copy(message = "Added ${result.friend.displayName ?: "friend"}.")
                    refresh()
                }
                AddFriendResult.NotFound -> _uiState.value = _uiState.value.copy(message = "No VitalCore user found with that code.")
                AddFriendResult.NotConfigured -> _uiState.value = _uiState.value.copy(message = "Sign in first to add friends.")
                is AddFriendResult.Error -> _uiState.value = _uiState.value.copy(message = result.message)
            }
        }
    }

    fun removeFriend(uid: String) = viewModelScope.launch {
        friendsRepository.removeFriend(uid)
        refresh()
    }
}
