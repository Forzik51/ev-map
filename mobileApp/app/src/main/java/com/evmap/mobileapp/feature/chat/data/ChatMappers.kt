package com.evmap.mobileapp.feature.chat.data

import com.evmap.mobileapp.core.data.dto.ChatDto
import com.evmap.mobileapp.core.data.dto.MessageDto
import core.ui.model.ChatUi
import core.ui.model.MessageUi
import com.evmap.mobileapp.feature.events.ui.displayDateTime
import java.time.Instant

fun ChatDto.toChatUi(): ChatUi =
    ChatUi(
        id = id.toString(),
        name = name,
        lastMessage = lastMessageText?.takeIf(String::isNotBlank) ?: when (lastAttachmentKind) {
            "IMAGE" -> "Photo"
            "VOICE" -> "Voice message"
            "FILE" -> "Attachment"
            else -> null
        },
        lastMessageTime = lastMessageAt?.displayDateTime(),
        unreadCount = unreadCount,
    )

fun MessageDto.toMessageUi(currentUserId: Long?): MessageUi =
    MessageUi(
        id = id.toString(),
        text = text,
        timestamp = runCatching { Instant.parse(sentAt).toEpochMilli() }.getOrDefault(System.currentTimeMillis()),
        isMine = senderId == currentUserId,
        senderName = senderUsername,
        attachment = attachment,
    )
