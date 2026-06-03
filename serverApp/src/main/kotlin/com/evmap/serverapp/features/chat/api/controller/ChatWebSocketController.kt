package com.evmap.serverapp.features.chat.api.controller

import com.evmap.serverapp.features.chat.api.dto.MarkChatReadRequest
import com.evmap.serverapp.features.chat.api.dto.SendMessageRequest
import com.evmap.serverapp.features.chat.api.dto.TypingEventRequest
import com.evmap.serverapp.features.chat.api.dto.ViewReadEvent
import com.evmap.serverapp.features.chat.api.dto.ViewTypingEvent
import com.evmap.serverapp.features.chat.application.command.MarkChatRead
import com.evmap.serverapp.features.chat.application.command.SendMessage
import com.evmap.serverapp.shared.security.CurrentUserProvider
import jakarta.validation.Valid
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller

@Controller
class ChatWebSocketController(
    private val sendMessage: SendMessage,
    private val markChatRead: MarkChatRead,
    private val messagingTemplate: SimpMessagingTemplate,
    private val currentUserProvider: CurrentUserProvider,
) {
    @MessageMapping("/chats/{chatId}/messages")
    fun send(
        @DestinationVariable chatId: Long,
        @Valid @Payload request: SendMessageRequest,
        authentication: Authentication,
    ) {
        val currentUser = currentUserProvider.fromAuthentication(authentication)
        val message = sendMessage.handle(chatId, currentUser.id, request.text)
        messagingTemplate.convertAndSend("/topic/chats/$chatId/messages", message)
    }

    @MessageMapping("/chats/{chatId}/typing")
    fun typing(
        @DestinationVariable chatId: Long,
        @Payload request: TypingEventRequest,
        authentication: Authentication,
    ) {
        val currentUser = currentUserProvider.fromAuthentication(authentication)
        messagingTemplate.convertAndSend(
            "/topic/chats/$chatId/typing",
            ViewTypingEvent(
                chatId = chatId,
                userId = currentUser.id,
                username = currentUser.username,
                typing = request.typing,
            ),
        )
    }

    @MessageMapping("/chats/{chatId}/read")
    fun read(
        @DestinationVariable chatId: Long,
        @Valid @Payload request: MarkChatReadRequest,
        authentication: Authentication,
    ) {
        val currentUser = currentUserProvider.fromAuthentication(authentication)
        markChatRead.handle(chatId, currentUser.id, request.lastReadMessageId)
        messagingTemplate.convertAndSend(
            "/topic/chats/$chatId/read",
            ViewReadEvent(
                chatId = chatId,
                userId = currentUser.id,
                lastReadMessageId = request.lastReadMessageId,
            ),
        )
    }
}
