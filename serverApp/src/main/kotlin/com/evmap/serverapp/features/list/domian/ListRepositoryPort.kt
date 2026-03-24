package com.evmap.serverapp.features.list.domian

interface ListRepositoryPort {
    fun save(list: UserList): Long
    fun addEvent(listId: Long, eventId: Long)
    fun removeEvent(listId: Long, eventId: Long)
    fun changeName(listId: Long, name: String)
    fun delete(listId: Long)
}
