package com.evmap.serverapp.features.event.application

class EventNotFoundException(id: Long) : RuntimeException("Event $id not found")
