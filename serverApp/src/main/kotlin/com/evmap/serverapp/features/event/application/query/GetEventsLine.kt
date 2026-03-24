package com.evmap.serverapp.features.event.application.query

import com.evmap.serverapp.features.event.api.dto.ViewEvent
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class GetEventsLine {
    fun handle(pageable: Pageable): Page<ViewEvent> = PageImpl(emptyList(), pageable, 0)
}
