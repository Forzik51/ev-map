package com.evmap.serverapp.features.list.infra.read

import com.evmap.serverapp.features.list.application.ListNotFoundException
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class ListReadRepository(
    private val dsl: DSLContext
) {
    fun findEventIds(listId: Long): List<Long> {
        val exists = dsl.fetchOne("SELECT 1 FROM user_list WHERE id = ?", listId) != null
        if (!exists) throw ListNotFoundException(listId)

        return dsl.fetch(
            """
            SELECT event_id
            FROM event_list
            WHERE favourite_list_id = ?
            ORDER BY event_id
            """.trimIndent(),
            listId
        ).mapNotNull { it.get("event_id", Long::class.java) }
    }
}
