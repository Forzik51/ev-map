package com.evmap.serverapp.features.event.domain

interface EventRepositoryPort {
    fun save(event: Event): Long
    fun update(eventId: Long, event: Event)
    fun delete(eventId: Long)
    fun addComment(eventId: Long, userId: Long, comment: String)
    fun addReaction(eventId: Long, userId: Long, reaction: String)
    fun addShare(eventId: Long, userId: Long)
    fun removeShare(eventId: Long, userId: Long)
}
