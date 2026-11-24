package com.evmap.serverapp.features.list.application.query

import com.evmap.serverapp.features.event.infra.read.EventReadRepository
import com.evmap.serverapp.features.list.api.dto.ViewSavedList
import com.evmap.serverapp.features.list.infra.read.ListReadRepository
import com.evmap.serverapp.shared.security.CurrentUserProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional

@Service
class GetSavedList(
    private val lists: ListReadRepository,
    private val events: EventReadRepository,
    private val currentUser: CurrentUserProvider,
) {
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    fun handle(listId: Long): ViewSavedList {
        val list = lists.findById(listId)
        currentUser.requireSelfOrAdmin(list.userId)
        return ViewSavedList(list, events.findSavedListViews(listId))
    }
}
