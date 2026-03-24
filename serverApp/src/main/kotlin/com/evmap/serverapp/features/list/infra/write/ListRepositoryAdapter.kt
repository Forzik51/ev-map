package com.evmap.serverapp.features.list.infra.write

import com.evmap.serverapp.features.list.application.ListNotFoundException
import com.evmap.serverapp.features.list.domian.ListRepositoryPort
import com.evmap.serverapp.features.list.domian.UserList
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class ListRepositoryAdapter(
    private val dsl: DSLContext
) : ListRepositoryPort {

    override fun save(list: UserList): Long =
        dsl.fetchOne(
            """
            INSERT INTO user_list(name, user_id)
            VALUES (?, ?)
            RETURNING id
            """.trimIndent(),
            list.name,
            list.userId
        )?.get("id", Long::class.java) ?: error("Failed to create user list")

    override fun addEvent(listId: Long, eventId: Long) {
        ensureListExists(listId)
        dsl.execute(
            """
            INSERT INTO event_list(favourite_list_id, event_id)
            VALUES (?, ?)
            ON CONFLICT DO NOTHING
            """.trimIndent(),
            listId,
            eventId
        )
    }

    override fun removeEvent(listId: Long, eventId: Long) {
        ensureListExists(listId)
        dsl.execute(
            "DELETE FROM event_list WHERE favourite_list_id = ? AND event_id = ?",
            listId,
            eventId
        )
    }

    override fun changeName(listId: Long, name: String) {
        val updated = dsl.execute("UPDATE user_list SET name = ? WHERE id = ?", name, listId)
        if (updated == 0) throw ListNotFoundException(listId)
    }

    @Transactional
    override fun delete(listId: Long) {
        ensureListExists(listId)
        dsl.execute("DELETE FROM event_list WHERE favourite_list_id = ?", listId)
        dsl.execute("DELETE FROM user_list WHERE id = ?", listId)
    }

    private fun ensureListExists(listId: Long) {
        val exists = dsl.fetchOne("SELECT 1 FROM user_list WHERE id = ?", listId) != null
        if (!exists) throw ListNotFoundException(listId)
    }
}
