package com.evmap.mobileapp.feature.chat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.evmap.mobileapp.BuildConfig
import com.evmap.mobileapp.core.data.dto.ChatAttachmentDto
import com.evmap.mobileapp.feature.chat.ui.components.VoiceRecordBar
import core.designsystem.Spacing
import feature.chat.ui.components.ConversationInputBar
import feature.chat.ui.components.MessageBubble
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    state: ChatUiState,
    chatId: Long,
    onBack: () -> Unit,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    onStartRecord: () -> Unit,
    onStopRecord: () -> Unit,
    onCancelRecord: () -> Unit,
    onDiscardAttachment: () -> Unit,
    onOlder: () -> Unit,
    onVisibleIncoming: (Long) -> Unit,
    onReconnect: () -> Unit,
    onAttachmentClick: (ChatAttachmentDto) -> Unit,
) {
    val conversation = state.conversation
    val messages = conversation.messages
    val listState = rememberLazyListState()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val dates = remember(messages) {
        messages.groupBy { Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate() }
            .entries.toList().asReversed()
    }
    val incoming = remember(messages) { messages.filterNot { it.isMine }.map { it.id }.asReversed() }

    LaunchedEffect(incoming, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            snapshotFlow {
                val layout = listState.layoutInfo
                val visible = layout.visibleItemsInfo.filter { item ->
                    val visiblePixels = minOf(item.offset + item.size, layout.viewportEndOffset) -
                        maxOf(item.offset, layout.viewportStartOffset)
                    visiblePixels * 2 >= minOf(item.size, layout.viewportEndOffset - layout.viewportStartOffset)
                }.mapTo(hashSetOf()) { it.key }
                incoming.firstOrNull { it in visible }?.toLongOrNull()
            }.filterNotNull().collect(onVisibleIncoming)
        }
    }
    LaunchedEffect(state.hasOlder, state.olderLoading, state.olderError, messages.firstOrNull()?.id) {
        if (state.hasOlder && !state.olderLoading && state.olderError == null) {
            snapshotFlow { listState.layoutInfo.visibleItemsInfo.any { it.key == OLDER_KEY } }.first { it }
            onOlder()
        }
    }
    LaunchedEffect(messages.lastOrNull()?.id) {
        if (messages.isNotEmpty() && listState.firstVisibleItemIndex <= 1) listState.scrollToItem(0)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        topBar = {
            TopAppBar(
                title = { Text(state.peerName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            state.connectionError?.let { error ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.m),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(error, Modifier.weight(1f), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onReconnect) { Text("Reconnect") }
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (state.loading) {
                    CircularProgressIndicator()
                } else if (messages.isEmpty()) {
                    if (state.connectionError == null) Text("No messages yet")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().testTag("chat_history"),
                        state = listState,
                        reverseLayout = true,
                        contentPadding = PaddingValues(vertical = Spacing.s),
                    ) {
                        dates.forEach { (date, dayMessages) ->
                            items(dayMessages.asReversed(), key = { it.id }) { message ->
                                val attachment = message.attachment
                                MessageBubble(
                                    message = message,
                                    attachmentUrl = attachment?.let {
                                        "${BuildConfig.BASE_URL.trimEnd('/')}/api/chats/$chatId/attachments/${it.id}"
                                    },
                                    playing = attachment != null && state.playingId == attachment.id,
                                    onAttachmentClick = { if (attachment != null) onAttachmentClick(attachment) },
                                )
                            }
                            item(key = "date_$date") {
                                Box(Modifier.fillMaxWidth().padding(vertical = Spacing.s), contentAlignment = Alignment.Center) {
                                    Text(
                                        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            }
                        }
                        if (state.hasOlder) {
                            item(key = OLDER_KEY) {
                                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                    if (state.olderLoading) {
                                        LinearProgressIndicator(Modifier.fillMaxWidth())
                                    } else {
                                        state.olderError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                                        TextButton(onClick = onOlder) {
                                            Text(if (state.olderError == null) "Load older messages" else "Retry older messages")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (state.typingNames.isNotEmpty()) {
                val names = state.typingNames.joinToString()
                Text(
                    if (state.typingNames.size == 1) "$names is typing…" else "$names are typing…",
                    Modifier.padding(horizontal = Spacing.m, vertical = Spacing.xs),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            state.error?.let { error ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.m),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(error, Modifier.weight(1f), color = MaterialTheme.colorScheme.error)
                    if (state.sendFailed) TextButton(onClick = onSend) { Text("Retry send") }
                }
            }
            if (conversation.isRecording) {
                VoiceRecordBar(conversation.recordSeconds, onStopRecord, onCancelRecord)
            } else {
                if (state.preparingMedia) LinearProgressIndicator(Modifier.fillMaxWidth())
                state.draft?.let { draft ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.m),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(draft.name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        TextButton(onClick = onDiscardAttachment, enabled = !state.sending && !state.preparingMedia) {
                            Text("Remove")
                        }
                        Button(onClick = onSend, enabled = !state.sending && !state.preparingMedia) {
                            Text("Send attachment")
                        }
                    }
                }
                state.uploadPercent?.let { progress ->
                    LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
                }
                ConversationInputBar(
                    text = conversation.input,
                    onTextChange = onInputChange,
                    onSend = onSend,
                    onAttach = onAttach,
                    onStartRecord = onStartRecord,
                    enabled = !state.loading && !state.sending && !state.preparingMedia,
                    canSendText = state.draft == null,
                )
            }
        }
    }
}

private const val OLDER_KEY = "older"
