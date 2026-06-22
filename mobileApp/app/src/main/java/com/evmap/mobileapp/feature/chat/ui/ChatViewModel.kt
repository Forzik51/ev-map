package com.evmap.mobileapp.feature.chat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evmap.mobileapp.core.data.remote.toUserMessage
import com.evmap.mobileapp.feature.chat.data.ChatRealtimeClient
import com.evmap.mobileapp.feature.chat.data.ChatRealtimeEvent
import com.evmap.mobileapp.core.ui.model.ChatsUiState
import com.evmap.mobileapp.core.ui.model.ConversationUi
import com.evmap.mobileapp.feature.chat.data.ChatRepository
import com.evmap.mobileapp.feature.chat.data.toChatUi
import com.evmap.mobileapp.feature.chat.data.toMessageUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val chats: ChatsUiState = ChatsUiState(),
    val conversation: ConversationUi = ConversationUi(),
    val peerName: String = "Chat",
    val loading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val realtimeClient: ChatRealtimeClient,
) : ViewModel() {
    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state
    private var realtimeJob: Job? = null
    private var activeChatId: Long? = null

    fun loadChats() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        repository.chats()
            .onSuccess { chats ->
                _state.update {
                    it.copy(chats = it.chats.copy(items = chats.map { chat -> chat.toChatUi() }), loading = false)
                }
            }
            .onFailure { error ->
                _state.update { it.copy(loading = false, error = error.toUserMessage()) }
            }
    }

    fun loadConversation(rawChatId: String) = viewModelScope.launch {
        val chatId = rawChatId.toLongOrNull() ?: return@launch
        activeChatId = chatId
        _state.update { it.copy(loading = true, error = null) }
        val chat = repository.chat(chatId).getOrNull()
        repository.messages(chatId)
            .onSuccess { messages ->
                val currentUserId = repository.currentUserId()
                _state.update {
                    it.copy(
                        peerName = chat?.name ?: "Chat",
                        conversation = it.conversation.copy(
                            messages = messages.map { message -> message.toMessageUi(currentUserId) },
                        ),
                        loading = false,
                    )
                }
                startRealtime(chatId)
            }
            .onFailure { error ->
                _state.update { it.copy(loading = false, error = error.toUserMessage()) }
            }
    }

    fun updateInput(value: String) {
        _state.update { it.copy(conversation = it.conversation.copy(input = value)) }
    }

    fun send(rawChatId: String) = viewModelScope.launch {
        val chatId = rawChatId.toLongOrNull() ?: return@launch
        val input = _state.value.conversation.input.trim()
        if (input.isBlank()) return@launch

        if (realtimeClient.sendMessage(chatId, input)) {
            _state.update { it.copy(conversation = it.conversation.copy(input = "")) }
            return@launch
        }

        repository.send(chatId, input)
            .onSuccess { message ->
                val currentUserId = repository.currentUserId()
                _state.update {
                    it.copy(
                        conversation = it.conversation.copy(
                            input = "",
                            messages = it.conversation.messages + message.toMessageUi(currentUserId),
                        )
                    )
                }
            }
            .onFailure { error ->
                _state.update { it.copy(error = error.toUserMessage()) }
            }
    }

    override fun onCleared() {
        realtimeJob?.cancel()
        realtimeClient.close()
        super.onCleared()
    }

    private fun startRealtime(chatId: Long) {
        realtimeJob?.cancel()
        realtimeJob = viewModelScope.launch {
            realtimeClient.connect(chatId).collect { event ->
                when (event) {
                    is ChatRealtimeEvent.Message -> {
                        val currentUserId = repository.currentUserId()
                        val message = event.message.toMessageUi(currentUserId)
                        _state.update { state ->
                            if (state.conversation.messages.any { it.id == message.id }) {
                                state
                            } else {
                                state.copy(
                                    conversation = state.conversation.copy(
                                        messages = state.conversation.messages + message,
                                        input = if (message.isMine) "" else state.conversation.input,
                                    )
                                )
                            }
                        }
                        realtimeClient.sendRead(chatId, event.message.id)
                    }
                }
            }
        }
    }

    fun enterSelectMode() {
        _state.update { it.copy(chats = it.chats.copy(isSelecting = true)) }
    }

    fun exitSelectMode() {
        _state.update { it.copy(chats = it.chats.copy(isSelecting = false, selection = emptySet())) }
    }

    fun toggleSelect(chatId: String) {
        _state.update { state ->
            val selection = if (chatId in state.chats.selection) {
                state.chats.selection - chatId
            } else {
                state.chats.selection + chatId
            }
            state.copy(chats = state.chats.copy(selection = selection))
        }
    }

    fun deleteSelected() = viewModelScope.launch {
        val selection = _state.value.chats.selection
        selection.mapNotNull { it.toLongOrNull() }.forEach { chatId ->
            repository.delete(chatId)
        }
        _state.update { state ->
            state.copy(
                chats = state.chats.copy(
                    items = state.chats.items.filterNot { it.id in selection },
                    isSelecting = false,
                    selection = emptySet(),
                )
            )
        }
    }
}
