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
import com.evmap.serverapp.features.list.domian.ListRepositoryPort
import com.evmap.serverapp.shared.security.CurrentUserProvider
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
    private val listRepository: ListRepositoryPort,
    private val currentUserProvider: CurrentUserProvider,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody dto: CreateList): Long =
        createList.handle(dto, currentUserProvider.get().id)

    @GetMapping
    fun getAll(): List<ViewList> = getAllLists.handle()

    @GetMapping("/users/{userId}")
    fun getAllByUser(@PathVariable userId: Long): List<ViewList> {
        currentUserProvider.requireSelfOrAdmin(userId)
        return getAllListsByUser.handle(userId)
    }

    @GetMapping("/me")
    fun getMine(): List<ViewList> = getAllListsByUser.handle(currentUserProvider.get().id)

    @GetMapping("/{listId}/events")
    fun getEvents(@PathVariable listId: Long): List<Long> {
        requireListOwnerOrAdmin(listId)
        return getEventsFromList.handle(listId)
    }

    @PostMapping("/{listId}/events/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun addEvent(@PathVariable listId: Long, @PathVariable eventId: Long) {
        requireListOwnerOrAdmin(listId)
        addEventToList.handle(listId, eventId)
    }

    @DeleteMapping("/{listId}/events/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeEvent(@PathVariable listId: Long, @PathVariable eventId: Long) {
        requireListOwnerOrAdmin(listId)
        removeEventFromList.handle(listId, eventId)
    }

    @PatchMapping("/{listId}/name")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun rename(@PathVariable listId: Long, @RequestParam name: String) {
        requireListOwnerOrAdmin(listId)
        changeListName.handle(listId, name)
    }

    @DeleteMapping("/{listId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable listId: Long) {
        requireListOwnerOrAdmin(listId)
        deleteList.handle(listId)
    }

    private fun requireListOwnerOrAdmin(listId: Long) {
        val currentUser = currentUserProvider.get()
        if (!currentUser.isAdmin && !listRepository.isOwner(listId, currentUser.id)) {
            throw org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Access denied")
        }
    }
}
