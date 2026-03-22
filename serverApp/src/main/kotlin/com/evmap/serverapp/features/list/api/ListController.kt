package com.evmap.serverapp.features.user.api


import com.evmap.serverapp.features.event.api.dto.CreateEvent
import com.evmap.serverapp.features.event.api.dto.ViewEvent
import com.evmap.serverapp.features.event.application.command.CreateEvent
import com.evmap.serverapp.features.event.application.query.GetAllEventsByUser
import com.evmap.serverapp.features.event.application.query.GetEventById
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/lists")
class ListController(
    private val createEvent: CreateEvent,
    private val getEventById: GetEventById,
    private val getAllEvents: GetAllEventsByUser
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody dto: CreateEvent): Long = createEvent.handle(dto)

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): ViewEvent = getEventById.handle(id)

    //@GetMapping
    //fun getAll(): List<EventView> = getAllEvents.handle()

    @GetMapping
    fun getAll(
        @PageableDefault(
            size = 20
            //, sort = ["createdAt"], direction = Sort.Direction.DESC
        )
        pageable: Pageable
    ): Page<ViewEvent> = getAllEvents.handle(pageable)
}