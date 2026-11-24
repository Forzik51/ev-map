package com.evmap.mobileapp.feature.chat.ui

import android.media.MediaPlayer
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evmap.mobileapp.core.data.dto.ChatAttachmentDto
import com.evmap.mobileapp.core.data.dto.ChatReadDto
import com.evmap.mobileapp.core.data.dto.ChatTypingDto
import com.evmap.mobileapp.core.data.dto.MessageDto
import com.evmap.mobileapp.core.ui.model.ChatsUiState
import com.evmap.mobileapp.core.ui.model.ConversationUi
import com.evmap.mobileapp.feature.chat.data.ChatMedia
import com.evmap.mobileapp.feature.chat.data.ChatMediaDraft
import com.evmap.mobileapp.feature.chat.data.ChatMediaKind
import com.evmap.mobileapp.feature.chat.data.ChatRealtimeClient
import com.evmap.mobileapp.feature.chat.data.ChatRealtimeEvent
import com.evmap.mobileapp.feature.chat.data.ChatRecording
import com.evmap.mobileapp.feature.chat.data.ChatRepository
import com.evmap.mobileapp.feature.chat.data.toChatUi
import com.evmap.mobileapp.feature.chat.data.toMessageUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

data class ChatUiState(
    val chats: ChatsUiState = ChatsUiState(),
    val conversation: ConversationUi = ConversationUi(),
    val peerName: String = "Chat",
    val loading: Boolean = false,
    val hasOlder: Boolean = false,
    val olderLoading: Boolean = false,
    val olderError: String? = null,
    val connected: Boolean = false,
    val connectionError: String? = null,
    val typingNames: List<String> = emptyList(),
    val sending: Boolean = false,
    val sendFailed: Boolean = false,
    val draft: ChatMediaDraft? = null,
    val preparingMedia: Boolean = false,
    val uploadPercent: Int? = null,
    val playingId: Long? = null,
    val error: String? = null,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val realtime: ChatRealtimeClient,
    private val media: ChatMedia,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val chatId = savedState.get<Long>("chatId")
    private val _state = MutableStateFlow(ChatUiState())
    val state = _state.asStateFlow()

    private val messages = mutableMapOf<Long, MessageDto>()
    private val readStates = mutableMapOf<Long, ChatReadDto>()
    private var members = emptySet<Long>()
    private var initialized = false
    // A live event can arrive out of order. Only a REST page advances the recovery cursor.
    private var syncCursor: Long? = null
    private var connectionJob: Job? = null
    private var pendingRead: MessageDto? = null
    private var readJob: Job? = null
    private val typing = MutableSharedFlow<Boolean>(extraBufferCapacity = 8)
    private var typingStop: Job? = null
    private var lastTyping: Long? = null
    private val peersTyping = mutableMapOf<Long, String>()
    private val typingExpiry = mutableMapOf<Long, Job>()
    private var pendingText: Pair<String, String>? = null
    private var recording: ChatRecording? = null
    private var recordingTimer: Job? = null
    private var player: MediaPlayer? = null
    private var playbackFile: File? = null
    private var playbackJob: Job? = null

    fun loadChats() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        repository.chats().onSuccess { chats ->
            _state.update { it.copy(chats = it.chats.copy(items = chats.map { chat -> chat.toChatUi() }), loading = false) }
        }.onFailure { error -> _state.update { it.copy(loading = false, error = error.chatMessage()) } }
    }

    fun resumeConversation() {
        val id = chatId ?: return
        if (connectionJob?.isActive == true) return
        _state.update { it.copy(loading = !initialized, connectionError = null) }
        connectionJob = viewModelScope.launch {
            var retryDelay = 1_000L
            while (isActive) {
                try {
                    // REST refreshes an expired access token before opening the STOMP session.
                    val chat = repository.chat(id).getOrThrow()
                    members = chat.userIds.toSet()
                    _state.update { it.copy(peerName = chat.name) }
                    if (!initialized) loadLatest(id)
                    realtime.connect(id, typing).collect { event ->
                        when (event) {
                            ChatRealtimeEvent.Ready -> {
                                catchUp(id)
                                repository.readStates(id).getOrThrow().forEach(::updateReceipt)
                                _state.update { it.copy(connected = true, connectionError = null) }
                                retryDelay = 1_000L
                                flushRead()
                            }
                            is ChatRealtimeEvent.Message -> if (event.message.chatId == id) merge(listOf(event.message))
                            is ChatRealtimeEvent.Typing -> if (event.event.chatId == id) updatePeerTyping(event.event)
                            is ChatRealtimeEvent.Read -> if (event.event.chatId == id) updateReceipt(event.event)
                        }
                    }
                    throw IOException("Chat connection closed")
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    clearPeerTyping()
                    _state.update { it.copy(loading = false, connected = false, connectionError = error.chatMessage()) }
                    if (error is HttpException && error.code() in setOf(401, 403, 404)) return@launch
                    delay(retryDelay)
                    retryDelay = (retryDelay * 2).coerceAtMost(20_000L)
                }
            }
        }
    }

    fun retryConnection() {
        connectionJob?.cancel()
        connectionJob = null
        resumeConversation()
    }

    fun pauseConversation() {
        stopTyping()
        connectionJob?.cancel()
        connectionJob = null
        readJob?.cancel()
        readJob = null
        clearPeerTyping()
        cancelRecording()
        stopPlayback()
        _state.update { it.copy(connected = false) }
    }

    private suspend fun loadLatest(id: Long, reset: Boolean = false) {
        val latest = repository.messages(id).getOrThrow()
        if (reset) messages.clear()
        syncCursor = latest.lastOrNull()?.id
        merge(latest)
        initialized = true
        _state.update { it.copy(loading = false, hasOlder = latest.size == ChatRepository.PAGE_SIZE) }
    }

    private suspend fun catchUp(id: Long) {
        var anchor = syncCursor
        if (anchor == null) {
            loadLatest(id)
            return
        }
        do {
            val result = repository.messages(id, afterId = anchor)
            val failure = result.exceptionOrNull()
            if (failure is HttpException && failure.code() == 404) {
                loadLatest(id, reset = true)
                return
            }
            val page = result.getOrThrow()
            merge(page)
            anchor = page.lastOrNull()?.id ?: anchor
            syncCursor = anchor
        } while (page.size == ChatRepository.PAGE_SIZE)
    }

    fun loadOlder() {
        val id = chatId ?: return
        val first = messages.values.minWithOrNull(messageOrder)?.id ?: return
        if (state.value.olderLoading || !state.value.hasOlder) return
        _state.update { it.copy(olderLoading = true, olderError = null) }
        viewModelScope.launch {
            repository.messages(id, beforeId = first).onSuccess { page ->
                merge(page)
                _state.update { it.copy(olderLoading = false, hasOlder = page.size == ChatRepository.PAGE_SIZE) }
            }.onFailure { error ->
                _state.update { it.copy(olderLoading = false, olderError = error.chatMessage()) }
            }
        }
    }

    fun updateInput(value: String) {
        _state.update { it.copy(conversation = it.conversation.copy(input = value)) }
        if (value.isBlank()) {
            stopTyping()
            return
        }
        val now = System.nanoTime()
        val previous = lastTyping
        if (previous == null || now - previous >= 1_000_000_000L) {
            typing.tryEmit(true)
            lastTyping = now
        }
        typingStop?.cancel()
        typingStop = viewModelScope.launch {
            delay(2_000)
            typing.tryEmit(false)
            lastTyping = null
        }
    }

    private fun stopTyping() {
        typingStop?.cancel()
        if (lastTyping != null) typing.tryEmit(false)
        lastTyping = null
    }

    private fun updatePeerTyping(event: ChatTypingDto) {
        typingExpiry.remove(event.userId)?.cancel()
        if (event.typing) {
            peersTyping[event.userId] = event.username ?: "Someone"
            typingExpiry[event.userId] = viewModelScope.launch {
                delay(4_000)
                typingExpiry.remove(event.userId)
                peersTyping.remove(event.userId)
                _state.update { it.copy(typingNames = peersTyping.values.toList()) }
            }
        } else peersTyping.remove(event.userId)
        _state.update { it.copy(typingNames = peersTyping.values.toList()) }
    }

    private fun clearPeerTyping() {
        typingExpiry.values.forEach(Job::cancel)
        typingExpiry.clear()
        peersTyping.clear()
        _state.update { it.copy(typingNames = emptyList()) }
    }

    fun visibleIncoming(messageId: Long) {
        val own = repository.currentUserId() ?: return
        val candidate = messages[messageId]?.takeUnless { it.senderId == own } ?: return
        if (readStates[own]?.covers(candidate) == true) return
        val previous = pendingRead
        if (previous == null || messageOrder.compare(previous, candidate) < 0) pendingRead = candidate
        flushRead()
    }

    private fun flushRead() {
        val id = chatId ?: return
        val own = repository.currentUserId() ?: return
        if (!state.value.connected || readJob?.isActive == true) return
        readJob = viewModelScope.launch {
            while (state.value.connected) {
                val target = pendingRead ?: break
                val result = repository.markRead(id, target.id)
                val error = result.exceptionOrNull()
                if (error == null) {
                    updateReceipt(ChatReadDto(id, own, target.id, target.sentAt))
                } else if (error is IOException || error is HttpException && error.code() >= 500) {
                    delay(2_000)
                    continue
                }
                if (pendingRead == target) pendingRead = null
            }
        }
    }

    private fun updateReceipt(event: ChatReadDto) {
        val previous = readStates[event.userId]
        if (previous == null || previous.position() < event.position()) {
            readStates[event.userId] = event
            renderMessages()
        }
    }

    private fun merge(page: List<MessageDto>) {
        page.forEach { messages[it.id] = it }
        renderMessages()
    }

    private fun renderMessages() {
        val ownId = repository.currentUserId()
        val peers = members.filter { it != ownId }
        val displayed = messages.values.sortedWith(messageOrder).map { message ->
            val ui = message.toMessageUi(ownId)
            ui.copy(isRead = ui.isMine && peers.isNotEmpty() && peers.all { readStates[it]?.covers(message) == true })
        }
        _state.update { it.copy(conversation = it.conversation.copy(messages = displayed)) }
    }

    fun send() {
        val id = chatId ?: return
        val snapshot = state.value
        if (snapshot.sending || snapshot.preparingMedia || snapshot.conversation.isRecording) return
        val attachment = snapshot.draft
        val text = snapshot.conversation.input.trim()
        if (attachment == null && text.isEmpty()) return
        if (attachment == null && text.length > 5000) {
            showError("Use up to 5000 characters.")
            return
        }
        stopTyping()
        _state.update { it.copy(sending = true, sendFailed = false, uploadPercent = if (attachment != null) 0 else null, error = null) }
        viewModelScope.launch {
            try {
                val message = if (attachment != null) {
                    val part = media.multipart(attachment) { percent -> _state.update { it.copy(uploadPercent = percent) } }
                    repository.upload(id, attachment, part).getOrThrow()
                } else {
                    val key = pendingText?.takeIf { it.first == text }?.second ?: UUID.randomUUID().toString()
                    pendingText = text to key
                    repository.send(id, text, key).getOrThrow()
                }
                merge(listOf(message))
                if (attachment != null) attachment.file.delete() else pendingText = null
                _state.update { current ->
                    val input = current.conversation.input
                    current.copy(
                        draft = null, sending = false, uploadPercent = null,
                        conversation = current.conversation.copy(input = if (attachment == null && input == snapshot.conversation.input) "" else input),
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update { it.copy(sending = false, sendFailed = true, uploadPercent = null, error = error.chatMessage()) }
            }
        }
    }

    fun chooseAttachment(uri: Uri, kind: ChatMediaKind) {
        if (state.value.sending || state.value.preparingMedia) return
        _state.update { it.copy(preparingMedia = true, error = null) }
        viewModelScope.launch {
            try {
                val draft = media.prepare(uri, kind)
                state.value.draft?.file?.delete()
                _state.update { it.copy(draft = draft, preparingMedia = false) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update { it.copy(preparingMedia = false, error = error.chatMessage()) }
            }
        }
    }

    fun discardAttachment() {
        if (state.value.sending) return
        state.value.draft?.file?.delete()
        _state.update { it.copy(draft = null, sendFailed = false, error = null) }
    }

    fun startRecording() {
        if (recording != null || state.value.sending || state.value.preparingMedia) return
        try {
            stopPlayback()
            val active = media.startRecording { stopRecording() }
            recording = active
            _state.update { it.copy(error = null, conversation = it.conversation.copy(isRecording = true, recordSeconds = 0)) }
            recordingTimer = viewModelScope.launch {
                while (isActive) {
                    delay(250)
                    _state.update { it.copy(conversation = it.conversation.copy(recordSeconds = (active.elapsedMs / 1000).toInt())) }
                }
            }
        } catch (error: Exception) {
            showError("Could not start the microphone.")
        }
    }

    fun stopRecording() {
        val active = detachRecording() ?: return
        try {
            val draft = active.finish()
            state.value.draft?.file?.delete()
            _state.update { it.copy(draft = draft) }
        } catch (error: Exception) {
            showError("Could not save this recording. Try again.")
        }
    }

    fun cancelRecording() { detachRecording()?.cancel() }

    private fun detachRecording(): ChatRecording? {
        val active = recording ?: return null
        recording = null
        recordingTimer?.cancel()
        _state.update { it.copy(conversation = it.conversation.copy(isRecording = false, recordSeconds = 0)) }
        return active
    }

    fun toggleVoice(attachment: ChatAttachmentDto) {
        val id = chatId ?: return
        val wasPlaying = state.value.playingId == attachment.id
        stopPlayback()
        if (wasPlaying) return
        _state.update { it.copy(playingId = attachment.id, error = null) }
        playbackJob = viewModelScope.launch {
            try {
                val file = media.cacheVoice(repository.download(id, attachment.id).getOrThrow())
                playbackFile = file
                player = MediaPlayer().also { audio ->
                    audio.setDataSource(file.absolutePath)
                    audio.setOnPreparedListener { it.start() }
                    audio.setOnCompletionListener { stopPlayback() }
                    audio.setOnErrorListener { _, _, _ ->
                        stopPlayback()
                        showError("Could not play this recording.")
                        true
                    }
                    audio.prepareAsync()
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                stopPlayback()
                showError(error.chatMessage())
            }
        }
    }

    private fun stopPlayback() {
        playbackJob?.cancel()
        player?.release()
        player = null
        playbackFile?.delete()
        playbackFile = null
        _state.update { it.copy(playingId = null) }
    }

    fun download(attachmentId: Long, destination: Uri) {
        val id = chatId ?: return
        viewModelScope.launch {
            try {
                media.saveFile(repository.download(id, attachmentId).getOrThrow(), destination)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                showError(error.chatMessage())
            }
        }
    }

    fun showError(message: String) { _state.update { it.copy(error = message) } }

    fun enterSelectMode() { _state.update { it.copy(chats = it.chats.copy(isSelecting = true)) } }
    fun exitSelectMode() { _state.update { it.copy(chats = it.chats.copy(isSelecting = false, selection = emptySet())) } }
    fun toggleSelect(id: String) {
        _state.update { state ->
            val selection = state.chats.selection
            state.copy(chats = state.chats.copy(selection = if (id in selection) selection - id else selection + id))
        }
    }

    fun deleteSelected() = viewModelScope.launch {
        val deleted = mutableSetOf<String>()
        for (id in state.value.chats.selection) {
            val number = id.toLongOrNull() ?: continue
            repository.delete(number).onSuccess { deleted += id }.onFailure { showError(it.chatMessage()) }
        }
        _state.update { it.copy(chats = it.chats.copy(items = it.chats.items.filterNot { row -> row.id in deleted })) }
        exitSelectMode()
    }

    override fun onCleared() {
        pauseConversation()
        state.value.draft?.file?.delete()
        super.onCleared()
    }
}

private val messageOrder = compareBy<MessageDto>({ Instant.parse(it.sentAt) }, { it.id })

private data class MessagePosition(val time: Instant, val id: Long) : Comparable<MessagePosition> {
    override fun compareTo(other: MessagePosition): Int = compareValuesBy(this, other, { it.time }, { it.id })
}

private fun ChatReadDto.position() = MessagePosition(Instant.parse(lastReadSentAt), lastReadMessageId)
private fun ChatReadDto.covers(message: MessageDto) = position() >= MessagePosition(Instant.parse(message.sentAt), message.id)

internal fun Throwable.chatMessage(): String = when (this) {
    is HttpException -> when (code()) {
        401 -> "Sign in again to continue."
        403 -> "You do not have access to this chat."
        404 -> "The chat or message is no longer available."
        413 -> "Choose a file up to 5 MB."
        400 -> "Check the message or attachment format."
        else -> "The server could not complete the request. Try again."
    }
    is IOException -> "Check your connection and try again."
    is IllegalArgumentException -> message ?: "Check the selected file."
    else -> "Could not complete the request. Try again."
}
