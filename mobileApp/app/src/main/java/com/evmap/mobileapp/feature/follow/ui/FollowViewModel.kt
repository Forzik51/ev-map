package com.evmap.mobileapp.feature.follow.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evmap.mobileapp.core.data.remote.toUserMessage
import com.evmap.mobileapp.feature.profile.data.UsersRepository
import com.evmap.mobileapp.feature.profile.data.toUserFollowUi
import core.ui.model.UserFollowUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FollowUiState(
    val users: List<UserFollowUi> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class FollowViewModel @Inject constructor(
    private val repository: UsersRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(FollowUiState())
    val state: StateFlow<FollowUiState> = _state

    fun loadFollowers(rawUserId: String) = load(rawUserId) { userId ->
        repository.getFollowers(userId)
    }

    fun loadFollowing(rawUserId: String) = load(rawUserId) { userId ->
        repository.getFollowing(userId)
    }

    fun toggleFollow(rawTargetUserId: String) = viewModelScope.launch {
        val targetUserId = rawTargetUserId.toLongOrNull() ?: return@launch
        val current = _state.value.users.firstOrNull { it.user.id == rawTargetUserId } ?: return@launch
        val result = if (current.isFollowing) repository.unfollow(targetUserId) else repository.follow(targetUserId)
        result
            .onSuccess {
                _state.update { state ->
                    state.copy(
                        users = state.users.map { item ->
                            if (item.user.id == rawTargetUserId) item.copy(isFollowing = !item.isFollowing) else item
                        }
                    )
                }
            }
            .onFailure { error ->
                _state.update { it.copy(error = error.toUserMessage()) }
            }
    }

    private fun load(
        rawUserId: String,
        request: suspend (Long) -> Result<List<com.evmap.mobileapp.core.data.dto.UserShortDto>>,
    ) = viewModelScope.launch {
        _state.value = FollowUiState(loading = true)
        val userId = rawUserId.toLongOrNull() ?: repository.currentUserId()
        if (userId == null) {
            _state.value = FollowUiState(error = "Missing user")
            return@launch
        }

        val currentFollowing = repository.currentUserId()
            ?.let { repository.getFollowing(it).getOrDefault(emptyList()).map { user -> user.id }.toSet() }
            .orEmpty()

        request(userId)
            .onSuccess { users ->
                _state.value = FollowUiState(
                    users = users.map { it.toUserFollowUi(isFollowing = it.id in currentFollowing) },
                    loading = false,
                )
            }
            .onFailure { error ->
                _state.value = FollowUiState(error = error.toUserMessage(), loading = false)
            }
    }
}
