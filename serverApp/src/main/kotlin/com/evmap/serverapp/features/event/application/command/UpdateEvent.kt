package com.evmap.serverapp.features.event.application.command

import com.evmap.serverapp.features.event.api.dto.CreateEvent
import org.springframework.stereotype.Service

@Service
class UpdateEvent {
    fun handle(eventId: Long, dto: CreateEvent) {
    }
}
