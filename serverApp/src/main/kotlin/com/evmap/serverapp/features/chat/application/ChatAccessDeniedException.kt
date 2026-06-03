package com.evmap.serverapp.features.chat.application

class ChatAccessDeniedException(chatId: Long) : RuntimeException("Access denied to chat $chatId")
