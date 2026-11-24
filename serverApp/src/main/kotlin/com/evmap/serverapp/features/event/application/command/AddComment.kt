package com.evmap.serverapp.features.event.application.command

import com.evmap.serverapp.features.event.domain.EventRepositoryPort
import org.springframework.stereotype.Service

@Service
class AddComment(
    private val repo: EventRepositoryPort,
) {
    fun handle(eventId: Long, userId: Long, comment: String) {
        val text = comment.trim()
        require(text.isNotEmpty()) { "Comment must not be empty" }
        require(text.codePointCount(0, text.length) <= 100) { "Comment must be at most 100 characters" }
        repo.addComment(eventId, userId, text)
    }
}
