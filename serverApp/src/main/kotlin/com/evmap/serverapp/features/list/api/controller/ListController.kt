package com.evmap.serverapp.features.list.api.controller

import com.evmap.serverapp.features.list.api.dto.CreateList
import com.evmap.serverapp.features.list.api.dto.ViewList
import com.evmap.serverapp.features.list.application.command.AddEventToList
import com.evmap.serverapp.features.list.application.command.ChangeListName
import com.evmap.serverapp.features.list.application.command.CreateList as CreateListCommand
import com.evmap.serverapp.features.list.application.command.DeleteList
import com.evmap.serverapp.features.list.application.command.RemoveEventFromList
import com.evmap.serverapp.features.list.application.query.GetAllLists
import com.evmap.serverapp.features.list.application.query.GetAllListsByUser
import com.evmap.serverapp.features.list.application.query.GetEventsFromList
import jakarta.validation.Valid
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
@RequestMapping("/api/lists")
class ListController(
    private val createList: CreateListCommand,
    private val getAllLists: GetAllLists,
    private val getAllListsByUser: GetAllListsByUser,
    private val addEventToList: AddEventToList,
    private val changeListName: ChangeListName,
    private val deleteList: DeleteList,
    private val removeEventFromList: RemoveEventFromList,
    private val getEventsFromList: GetEventsFromList,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody dto: CreateList): Long = createList.handle(dto)

    @GetMapping
    fun getAll(): List<ViewList> = getAllLists.handle()

    @GetMapping("/users/{userId}")
    fun getAllByUser(@PathVariable userId: Long): List<ViewList> = getAllListsByUser.handle(userId)

    @GetMapping("/{listId}/events")
    fun getEvents(@PathVariable listId: Long): List<Long> = getEventsFromList.handle(listId)

    @PostMapping("/{listId}/events/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun addEvent(@PathVariable listId: Long, @PathVariable eventId: Long) {
        addEventToList.handle(listId, eventId)
    }

    @DeleteMapping("/{listId}/events/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeEvent(@PathVariable listId: Long, @PathVariable eventId: Long) {
        removeEventFromList.handle(listId, eventId)
    }

    @PatchMapping("/{listId}/name")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun rename(@PathVariable listId: Long, @RequestParam name: String) {
        changeListName.handle(listId, name)
    }

    @DeleteMapping("/{listId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable listId: Long) {
        deleteList.handle(listId)
    }
}
