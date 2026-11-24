package com.evmap.mobileapp.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.evmap.mobileapp.core.data.remote.toUserMessage
import com.evmap.mobileapp.feature.chat.data.ChatRepository
import com.evmap.mobileapp.feature.chat.ui.chatMessage
import com.evmap.mobileapp.feature.events.data.EventsRepository
import com.evmap.mobileapp.feature.feed.data.toFeedItemUi
import com.evmap.mobileapp.feature.profile.data.UsersRepository
import com.evmap.mobileapp.feature.profile.data.toProfileUserUi
import core.ui.model.ProfileUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: UsersRepository,
    private val chats: ChatRepository,
    eventsRepository: EventsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState(isLoading = true))
    val state: StateFlow<ProfileUiState> = _state

    @OptIn(ExperimentalCoroutinesApi::class)
    val events = state.map { it.user?.id?.toLongOrNull() }.distinctUntilChanged().flatMapLatest { userId ->
        if (userId == null) flowOf(PagingData.empty())
        else eventsRepository.byAuthor(userId).map { page -> page.map { it.toFeedItemUi() } }
    }.cachedIn(viewModelScope)

    private var loadedUserId: Long? = null

    fun loadMe() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null, isMe = true) }
        repository.getMe()
            .onSuccess { user ->
                loadedUserId = user.id
                _state.update {
                    it.copy(
                        user = user.toProfileUserUi(),
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
        if (state.value.isChangingFollow) return@launch
        val currentUserId = repository.currentUserId()
        val isMe = currentUserId == userId
        _state.update { it.copy(isLoading = true, error = null, isMe = isMe) }
        repository.getUser(userId)
            .onSuccess { user ->
                loadedUserId = user.id
                val following = currentUserId?.takeUnless { isMe }?.let { repository.getFollowing(it) }

                _state.update {
                    it.copy(
                        user = user.toProfileUserUi(),
                        isMe = isMe,
                        isFollowing = following?.getOrNull()?.any { it.id == user.id } == true,
                        followStatusAvailable = currentUserId != null && following?.isFailure != true,
                        isLoading = false,
                        error = following?.exceptionOrNull()?.toUserMessage(),
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
        if (current.isMe || current.isLoading || current.isChangingFollow || !current.followStatusAvailable) return@launch
        _state.update { it.copy(isChangingFollow = true, error = null) }

        val result = if (current.isFollowing) {
            repository.unfollow(targetUserId)
        } else {
            repository.follow(targetUserId)
        }

        result
            .onSuccess {
                _state.update { state ->
                    val followersDelta = if (current.isFollowing) -1 else 1
                    state.copy(
                        isFollowing = !current.isFollowing,
                        isChangingFollow = false,
                        user = state.user?.copy(followers = (state.user.followers + followersDelta).coerceAtLeast(0))
                    )
                }
            }
            .onFailure { error ->
                _state.update { it.copy(isChangingFollow = false, error = error.toUserMessage()) }
            }
    }

    fun toggleView() {
        _state.update { it.copy(selectedViewIsGrid = !it.selectedViewIsGrid) }
    }

    fun openChat() = viewModelScope.launch {
        val peerId = loadedUserId ?: return@launch
        val current = _state.value
        if (current.isMe || current.isLoading || current.openingChat || current.openedChatId != null) return@launch
        _state.update { it.copy(openingChat = true, error = null) }
        chats.openDirect(peerId)
            .onSuccess { chatId -> _state.update { it.copy(openingChat = false, openedChatId = chatId) } }
            .onFailure { error -> _state.update { it.copy(openingChat = false, error = error.chatMessage()) } }
    }

    fun consumeOpenedChat() {
        _state.update { it.copy(openedChatId = null) }
    }
}
