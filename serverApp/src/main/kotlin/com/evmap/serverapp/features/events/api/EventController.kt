package com.evmap.serverapp.features.events.api

import com.evmap.serverapp.features.events.api.dto.CreateEventDto
import com.evmap.serverapp.features.events.api.dto.EventView
import com.evmap.serverapp.features.events.application.command.CreateEventCommand
import com.evmap.serverapp.features.events.application.query.GetAllEventsQuery
import com.evmap.serverapp.features.events.application.query.GetEventByIdQuery
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault

@RestController
@RequestMapping("/api/events")
class EventController(
    private val createEvent: CreateEventCommand,
    private val getEventById: GetEventByIdQuery,
    private val getAllEvents: GetAllEventsQuery
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody dto: CreateEventDto): Long = createEvent.handle(dto)

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): EventView = getEventById.handle(id)

    //@GetMapping
    //fun getAll(): List<EventView> = getAllEvents.handle()

    @GetMapping
    fun getAll(
        @PageableDefault(
            size = 20
            //, sort = ["createdAt"], direction = Sort.Direction.DESC
        )
        pageable: Pageable
    ): Page<EventView> = getAllEvents.handle(pageable)
}