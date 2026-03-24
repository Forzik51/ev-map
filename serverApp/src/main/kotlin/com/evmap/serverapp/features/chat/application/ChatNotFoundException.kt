package com.evmap.serverapp.features.chat.application

class ChatNotFoundException(id: Long) : RuntimeException("Chat $id not found")
