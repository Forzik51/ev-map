package com.evmap.mobileapp.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evmap.mobileapp.core.data.remote.toUserMessage
import com.evmap.mobileapp.feature.feed.data.toFeedItemUi
import com.evmap.mobileapp.feature.profile.data.UsersRepository
import com.evmap.mobileapp.feature.profile.data.toProfileUserUi
import core.ui.model.ProfileUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: UsersRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState(isLoading = true))
    val state: StateFlow<ProfileUiState> = _state

    private var loadedUserId: Long? = null

    fun loadMe() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null, isMe = true) }
        repository.getMe()
            .onSuccess { user ->
                loadedUserId = user.id
                val events = repository.getUserEvents(user.id).getOrDefault(emptyList())
                _state.update {
                    it.copy(
                        user = user.toProfileUserUi(),
                        list = events.map { event -> event.toFeedItemUi() },
                        isMe = true,
                        isFollowing = false,
                        isLoading = false,
                    )
                }
            }
            .onFailure { error ->
                _state.update { it.copy(isLoading = false, error = error.toUserMessage()) }
            }
    }

    fun loadUser(userId: Long) = viewModelScope.launch {
        val currentUserId = repository.currentUserId()
        val isMe = currentUserId == userId
        _state.update { it.copy(isLoading = true, error = null, isMe = isMe) }
        repository.getUser(userId)
            .onSuccess { user ->
                loadedUserId = user.id
                val events = repository.getUserEvents(user.id).getOrDefault(emptyList())
                val isFollowing = currentUserId?.let { current ->
                    repository.getFollowing(current)
                        .getOrDefault(emptyList())
                        .any { it.id == user.id }
                } ?: false

                _state.update {
                    it.copy(
                        user = user.toProfileUserUi(),
                        list = events.map { event -> event.toFeedItemUi() },
                        isMe = isMe,
                        isFollowing = isFollowing,
                        isLoading = false,
                    )
                }
            }
            .onFailure { error ->
                _state.update { it.copy(isLoading = false, error = error.toUserMessage()) }
            }
    }

    fun toggleFollow() = viewModelScope.launch {
        val targetUserId = loadedUserId ?: return@launch
        val current = _state.value
        if (current.isMe) return@launch

        val result = if (current.isFollowing) {
            repository.unfollow(targetUserId)
        } else {
            repository.follow(targetUserId)
        }

        result
            .onSuccess {
                _state.update { state ->
                    val followersDelta = if (state.isFollowing) -1 else 1
                    state.copy(
                        isFollowing = !state.isFollowing,
                        user = state.user?.copy(followers = (state.user.followers + followersDelta).coerceAtLeast(0))
                    )
                }
            }
            .onFailure { error ->
                _state.update { it.copy(error = error.toUserMessage()) }
            }
    }

    fun toggleView() {
        _state.update { it.copy(selectedViewIsGrid = !it.selectedViewIsGrid) }
    }
}
