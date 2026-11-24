package com.evmap.serverapp.features.chat.api.controller

import com.evmap.serverapp.features.chat.api.dto.ViewMessage
import com.evmap.serverapp.features.chat.api.dto.ViewReadEvent
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ChatBroadcast(private val messaging: SimpMessagingTemplate) {
    @TransactionalEventListener
    fun message(event: ViewMessage) = messaging.convertAndSend("/topic/chats/${event.chatId}/messages", event)

    @TransactionalEventListener
    fun read(event: ViewReadEvent) = messaging.convertAndSend("/topic/chats/${event.chatId}/read", event)
}
