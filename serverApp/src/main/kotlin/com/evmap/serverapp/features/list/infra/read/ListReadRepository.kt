package com.evmap.serverapp.features.list.infra.read

import com.evmap.serverapp.features.list.api.dto.ViewList
import com.evmap.serverapp.features.list.application.ListNotFoundException
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class ListReadRepository(
    private val dsl: DSLContext
) {
    fun findAll(): List<ViewList> =
        dsl.fetch(
            """
            SELECT ul.id,
                   ul.name,
                   ul.user_id,
                   COUNT(el.event_id) AS events_count
            FROM user_list ul
            LEFT JOIN event_list el ON el.favourite_list_id = ul.id
            GROUP BY ul.id, ul.name, ul.user_id
            ORDER BY ul.id DESC
            """.trimIndent()
        ).map { toViewList(it) }

    fun findAllByUserId(userId: Long): List<ViewList> =
        dsl.fetch(
            """
            SELECT ul.id,
                   ul.name,
                   ul.user_id,
                   COUNT(el.event_id) AS events_count
            FROM user_list ul
            LEFT JOIN event_list el ON el.favourite_list_id = ul.id
            WHERE ul.user_id = ?
            GROUP BY ul.id, ul.name, ul.user_id
            ORDER BY ul.id DESC
            """.trimIndent(),
            userId
        ).map { toViewList(it) }

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

    private fun toViewList(record: org.jooq.Record): ViewList =
        ViewList(
            id = record.get("id", Long::class.java)!!,
            name = record.get("name", String::class.java)!!,
            userId = record.get("user_id", Long::class.java)!!,
            eventsCount = (record.get("events_count") as? Number)?.toInt() ?: 0,
        )
}
