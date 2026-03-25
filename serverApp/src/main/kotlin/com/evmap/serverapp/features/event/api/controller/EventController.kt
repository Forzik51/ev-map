package com.evmap.serverapp.features.event.api.controller

import com.evmap.serverapp.features.event.api.dto.CreateEvent
import com.evmap.serverapp.features.event.api.dto.ViewEvent
import com.evmap.serverapp.features.event.api.dto.ViewEventCategory
import com.evmap.serverapp.features.event.api.dto.ViewEventComment
import com.evmap.serverapp.features.event.api.dto.ViewEventReaction
import com.evmap.serverapp.features.event.application.command.AddComment
import com.evmap.serverapp.features.event.application.command.AddReaction
import com.evmap.serverapp.features.event.application.command.AddShare
import com.evmap.serverapp.features.event.application.command.CreateEvent as CreateEventCommand
import com.evmap.serverapp.features.event.application.command.DeleteEvent
import com.evmap.serverapp.features.event.application.command.RemoveShare
import com.evmap.serverapp.features.event.application.command.UpdateEvent
import com.evmap.serverapp.features.event.application.query.GetAllEventCategories
import com.evmap.serverapp.features.event.application.query.GetAllEventsByUser
import com.evmap.serverapp.features.event.application.query.GetCommentsByEventId
import com.evmap.serverapp.features.event.application.query.GetEventById
import com.evmap.serverapp.features.event.application.query.GetEventsLine
import com.evmap.serverapp.features.event.application.query.GetReactionsByEventId
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/events")
class EventController(
    private val createEvent: CreateEventCommand,
    private val getAllEventCategories: GetAllEventCategories,
    private val getEventById: GetEventById,
    private val getAllEventsByUser: GetAllEventsByUser,
    private val getEventsLine: GetEventsLine,
    private val getCommentsByEventId: GetCommentsByEventId,
    private val getReactionsByEventId: GetReactionsByEventId,
    private val updateEvent: UpdateEvent,
    private val deleteEvent: DeleteEvent,
    private val addComment: AddComment,
    private val addReaction: AddReaction,
    private val addShare: AddShare,
    private val removeShare: RemoveShare,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody dto: CreateEvent): Long = createEvent.handle(dto)

    @GetMapping("/categories")
    fun getAllCategories(): List<ViewEventCategory> = getAllEventCategories.handle()

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ViewEvent = getEventById.handle(id)

    @GetMapping
    fun getAll(
        @PageableDefault(size = 20)
        pageable: Pageable
    ): Page<ViewEvent> = getAllEventsByUser.handle(pageable)

    @GetMapping("/line")
    fun getLine(
        @PageableDefault(size = 20)
        pageable: Pageable
    ): Page<ViewEvent> = getEventsLine.handle(pageable)

    @GetMapping("/{id}/comments")
    fun getCommentsByEvent(
        @PathVariable id: Long,
        @PageableDefault(size = 20)
        pageable: Pageable
    ): Page<ViewEventComment> = getCommentsByEventId.handle(id, pageable)

    @GetMapping("/{id}/reactions")
    fun getReactionsByEvent(
        @PathVariable id: Long,
        @PageableDefault(size = 20)
        pageable: Pageable
    ): Page<ViewEventReaction> = getReactionsByEventId.handle(id, pageable)

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun update(@PathVariable id: Long, @Valid @RequestBody dto: CreateEvent) {
        updateEvent.handle(id, dto)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) {
        deleteEvent.handle(id)
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun comment(
        @PathVariable id: Long,
        @RequestParam userId: Long,
        @RequestParam comment: String,
    ) {
        addComment.handle(id, userId, comment)
    }

    @PostMapping("/{id}/reactions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun react(
        @PathVariable id: Long,
        @RequestParam userId: Long,
        @RequestParam reaction: String,
    ) {
        addReaction.handle(id, userId, reaction)
    }

    @PostMapping("/{id}/shares/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun share(@PathVariable id: Long, @PathVariable userId: Long) {
        addShare.handle(id, userId)
    }

    @DeleteMapping("/{id}/shares/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun unshare(@PathVariable id: Long, @PathVariable userId: Long) {
        removeShare.handle(id, userId)
    }
}
