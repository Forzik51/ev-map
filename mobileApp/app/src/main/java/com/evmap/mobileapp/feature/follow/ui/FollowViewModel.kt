package com.evmap.mobileapp.feature.follow.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evmap.mobileapp.core.data.remote.toUserMessage
import com.evmap.mobileapp.feature.profile.data.UsersRepository
import com.evmap.mobileapp.feature.profile.data.toUserFollowUi
import core.ui.model.UserFollowUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FollowList(val segment: String, val title: String) {
    FOLLOWERS("followers", "Followers"), FOLLOWING("following", "Following"),
}

data class FollowUiState(
    val users: List<UserFollowUi> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val changing: Set<String> = emptySet(),
    val viewerId: String? = null,
)

@HiltViewModel
class FollowViewModel @Inject constructor(private val repository: UsersRepository) : ViewModel() {
    private val _state = MutableStateFlow(FollowUiState())
    val state = _state.asStateFlow()
    private var ownFollowing = false

    fun load(rawUserId: String, list: FollowList) {
        if (state.value.loading || state.value.changing.isNotEmpty()) return
        val viewer = repository.currentUserId()
        val owner = if (rawUserId == "me") viewer else rawUserId.toLongOrNull()
        if (owner == null) {
            _state.update { it.copy(error = "Missing user") }
            return
        }
        ownFollowing = owner == viewer && list == FollowList.FOLLOWING
        _state.update { it.copy(loading = true, error = null, viewerId = viewer?.toString()) }
        viewModelScope.launch {
            try {
                val users = when (list) {
                    FollowList.FOLLOWERS -> repository.getFollowers(owner)
                    FollowList.FOLLOWING -> repository.getFollowing(owner)
                }.getOrThrow()
                val following = if (ownFollowing) users else viewer?.let { repository.getFollowing(it).getOrThrow() }.orEmpty()
                val followingIds = following.map { it.id }.toSet()
                _state.update { it.copy(users = users.map { user -> user.toUserFollowUi(user.id in followingIds) }, loading = false) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update { it.copy(loading = false, error = error.toUserMessage()) }
            }
        }
    }

    fun toggleFollow(rawTargetId: String) {
        val snapshot = state.value
        val targetId = rawTargetId.toLongOrNull() ?: return
        if (snapshot.loading || snapshot.viewerId == null || snapshot.viewerId == rawTargetId || rawTargetId in snapshot.changing) return
        val user = snapshot.users.firstOrNull { it.user.id == rawTargetId } ?: return

        _state.update { it.copy(changing = it.changing + rawTargetId, error = null) }
        viewModelScope.launch {
            val result = if (user.isFollowing) repository.unfollow(targetId) else repository.follow(targetId)
            result.onSuccess {
                _state.update { current ->
                    val users = if (ownFollowing && user.isFollowing) {
                        current.users.filterNot { it.user.id == rawTargetId }
                    } else {
                        current.users.map { if (it.user.id == rawTargetId) it.copy(isFollowing = !user.isFollowing) else it }
                    }
                    current.copy(users = users, changing = current.changing - rawTargetId)
                }
            }.onFailure { error ->
                _state.update { it.copy(changing = it.changing - rawTargetId, error = error.toUserMessage()) }
            }
        }
    }
}
