@file:Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package com.evmap.mobileapp.feature.chat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.evmap.mobileapp.core.ui.model.ConversationUi
import core.designsystem.Spacing
import feature.chat.ui.components.ConversationInputBar
import feature.chat.ui.components.DateDivider
import feature.chat.ui.components.MessageBubble
import com.evmap.mobileapp.feature.chat.ui.components.VoiceRecordBar
import core.designsystem.AppTheme
import core.ui.model.MessageUi
import java.text.SimpleDateFormat
import java.util.*




@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun ConversationScreenPreview() {
    // ----- sample messages -----
    val now = System.currentTimeMillis()
    // Replace MessageUi(...) with your actual message model type/constructor
    val m1 = MessageUi(
        id = "m1",
        text = "Hey! Are we still on for tonight?",
        timestamp = now - 1000L * 60 * 60 * 24, // yesterday
        isMine = false,
        mediaUrl = null
    )
    val m2 = MessageUi(
        id = "m2",
        text = "Yep, see you at 19:00 🙂",
        timestamp = now - 1000L * 60 * 30, // 30 min ago
        isMine = true,
        mediaUrl = null
    )
    val m3 = MessageUi(
        id = "m3",
        text = null,
        timestamp = now - 1000L * 60 * 5, // 5 min ago
        isMine = false,
        mediaUrl = "" // image example
    )

    // ----- sample UI state -----
    val ui = ConversationUi(
        messages = listOf(m1, m2, m3),
        input = "Typing a reply…",
        isRecording = false,
        recordSeconds = 0
    )

    AppTheme {
        ConversationScreen(
            ui = ui,
            peerName = "Alice Johnson",
            onBack = {},
            onInputChange = {},
            onSend = {},
            onAttach = {},
            onStartRecord = {},
            onStopRecord = {},
            onCancelRecord = {},
            modifier = Modifier
        )
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    ui: ConversationUi,
    peerName: String,
    onBack: () -> Unit,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    onStartRecord: () -> Unit,
    onStopRecord: () -> Unit,
    onCancelRecord: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Auto-scroll to bottom when new messages arrive or when input changes (keyboard interaction)
    LaunchedEffect(ui.messages.size, ui.input) {
        if (ui.messages.isNotEmpty()) {
            listState.animateScrollToItem(ui.messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
    ) {
        // Top app bar
        TopAppBar(
            title = {
                Text(
                    text = peerName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Message list
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            state = listState,
            contentPadding = PaddingValues(
                start = Spacing.m,
                end = Spacing.m,
                bottom = Spacing.m
            )
        ) {
            // Group messages by day
            val groupedMessages = ui.messages.groupBy { message ->
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(message.timestamp))
            }

            groupedMessages.forEach { (date, messagesForDay) ->
                // Date divider
                item(key = "date_$date") {
                    DateDivider(
                        text = formatDateForDisplay(date)
                    )
                }

                // Messages for this day
                items(
                    items = messagesForDay,
                    key = { message -> message.id }
                ) { message ->
                    MessageBubble(
                        message = message,
                        onClickMedia = { mediaUrl ->
                            // TODO: Handle media click
                        }
                    )
                }
            }
        }

        // Bottom input area
        if (ui.isRecording) {
            VoiceRecordBar(
                seconds = ui.recordSeconds,
                onStop = onStopRecord,
                onCancel = onCancelRecord
            )
        } else {
            ConversationInputBar(
                text = ui.input,
                onTextChange = onInputChange,
                onSend = onSend,
                onAttach = onAttach,
                onStartRecord = onStartRecord
            )
        }
    }
}

private fun formatDateForDisplay(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val messageDate = Calendar.getInstance().apply {
            if (date != null) {
                time = date
            }
        }
        
        when {
            isSameDay(messageDate, today) -> "Today"
            isSameDay(messageDate, yesterday) -> "Yesterday"
            else -> outputFormat.format(date)
        }
    } catch (e: Exception) {
        dateString
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
