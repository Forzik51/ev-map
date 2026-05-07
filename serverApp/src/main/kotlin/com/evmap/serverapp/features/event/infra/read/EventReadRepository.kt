package com.evmap.serverapp.features.event.infra.read

import com.evmap.serverapp.features.event.api.dto.ViewEvent
import com.evmap.serverapp.features.event.api.dto.ViewEventCategory
import com.evmap.serverapp.features.event.api.dto.ViewEventComment
import com.evmap.serverapp.features.event.api.dto.ViewEventReaction
import com.evmap.serverapp.features.event.application.EventNotFoundException
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.sql.Timestamp

@Repository
class EventReadRepository(private val dsl: DSLContext) {

    fun findAllCategories(): List<ViewEventCategory> =
        dsl.fetch(
            """
            SELECT id, name
            FROM category
            ORDER BY id
            """.trimIndent()
        ).map {
            ViewEventCategory(
                id = requireNotNull(it.get("id", Long::class.java)) { "id is null for category row" },
                name = requireNotNull(it.get("name", String::class.java)) { "name is null for category row" },
            )
        }

    fun findViewById(id: Long): ViewEvent =
        dsl.fetchOne(
            """
            SELECT e.id,
                   e.name,
                   e.description,
                   e.start_at,
                   e.end_at,
                   e.user_id,
                   l.name AS location_name,
                   ph.image_path AS image_path,
                   COALESCE(rp.reposts_count, 0) AS reposts_count,
                   COALESCE(rt.ratings_count, 0) AS ratings_count,
                   COALESCE(cm.comments_count, 0) AS comments_count
            FROM event e
            LEFT JOIN location l ON l.id = e.location_id
            LEFT JOIN LATERAL (
                SELECT p.image_path
                FROM photo p
                WHERE p.event_id = e.id
                ORDER BY p.id
                LIMIT 1
            ) ph ON TRUE
            LEFT JOIN (
                SELECT event_id, COUNT(*) AS reposts_count
                FROM repost
                GROUP BY event_id
            ) rp ON rp.event_id = e.id
            LEFT JOIN (
                SELECT event_id, COUNT(*) AS ratings_count
                FROM rating
                GROUP BY event_id
            ) rt ON rt.event_id = e.id
            LEFT JOIN (
                SELECT event_id, COUNT(*) AS comments_count
                FROM comment
                GROUP BY event_id
            ) cm ON cm.event_id = e.id
            WHERE e.id = ?
            """.trimIndent(),
            id
        )?.let { r ->
            val startAt = r.get("start_at", Timestamp::class.java)
                ?: throw IllegalStateException("start_at is null for id=$id")
            val endAt = r.get("end_at", Timestamp::class.java)
            val categoriesByEventId = findCategoriesByEventIds(listOf(id))

            ViewEvent(
                id = requireNotNull(r.get("id", Long::class.java)) { "id is null for id=$id" },
                name = requireNotNull(r.get("name", String::class.java)) { "name is null for id=$id" },
                description = requireNotNull(r.get("description", String::class.java)) { "description is null for id=$id" },
                startsAt = startAt.toInstant().toString(),
                endsAt = endAt?.toInstant()?.toString(),
                locationName = r.get("location_name", String::class.java),
                imagePath = r.get("image_path", String::class.java),
                userId = requireNotNull(r.get("user_id", Long::class.java)) { "user_id is null for id=$id" },
                repostsCount = countValue(r, "reposts_count"),
                ratingsCount = countValue(r, "ratings_count"),
                commentsCount = countValue(r, "comments_count"),
                categories = categoriesByEventId[id].orEmpty(),
            )
        } ?: throw EventNotFoundException(id)

    fun findAllViews(pageable: Pageable): Page<ViewEvent> {
        val sql = """
            SELECT e.id,
                   e.name,
                   e.description,
                   e.start_at,
                   e.end_at,
                   e.user_id,
                   l.name AS location_name,
                   ph.image_path AS image_path,
                   COALESCE(rp.reposts_count, 0) AS reposts_count,
                   COALESCE(rt.ratings_count, 0) AS ratings_count,
                   COALESCE(cm.comments_count, 0) AS comments_count
            FROM event e
            LEFT JOIN location l ON l.id = e.location_id
            LEFT JOIN LATERAL (
                SELECT p.image_path
                FROM photo p
                WHERE p.event_id = e.id
                ORDER BY p.id
                LIMIT 1
            ) ph ON TRUE
            LEFT JOIN (
                SELECT event_id, COUNT(*) AS reposts_count
                FROM repost
                GROUP BY event_id
            ) rp ON rp.event_id = e.id
            LEFT JOIN (
                SELECT event_id, COUNT(*) AS ratings_count
                FROM rating
                GROUP BY event_id
            ) rt ON rt.event_id = e.id
            LEFT JOIN (
                SELECT event_id, COUNT(*) AS comments_count
                FROM comment
                GROUP BY event_id
            ) cm ON cm.event_id = e.id
            ORDER BY e.start_at DESC
            LIMIT ? OFFSET ?
        """.trimIndent()

        val recs = dsl.resultQuery(sql, pageable.pageSize, pageable.offset).fetch()

        val eventIds = recs.mapNotNull { it.get("id", Long::class.java) }
        val categoriesByEventId = findCategoriesByEventIds(eventIds)

        val items = recs.map {
            val eventId = requireNotNull(it.get("id", Long::class.java)) { "id is null for event row" }
            ViewEvent(
                id = eventId,
                name = requireNotNull(it.get("name", String::class.java)) { "name is null for event row" },
                description = requireNotNull(it.get("description", String::class.java)) { "description is null for event row" },
                startsAt = requireNotNull(it.get("start_at", Timestamp::class.java)) { "start_at is null for event row" }
                    .toInstant().toString(),
                endsAt = it.get("end_at", Timestamp::class.java)?.toInstant()?.toString(),
                locationName = it.get("location_name", String::class.java),
                imagePath = it.get("image_path", String::class.java),
                userId = requireNotNull(it.get("user_id", Long::class.java)) { "user_id is null for event row" },
                repostsCount = countValue(it, "reposts_count"),
                ratingsCount = countValue(it, "ratings_count"),
                commentsCount = countValue(it, "comments_count"),
                categories = categoriesByEventId[eventId].orEmpty(),
            )
        }

        val total = dsl.fetchOne(
            """
            SELECT COUNT(*)
            FROM event
            """.trimIndent()
        )?.get(0, Long::class.java) ?: 0L

        return PageImpl(items, pageable, total)
    }

    fun findCommentsByEventId(eventId: Long, pageable: Pageable): Page<ViewEventComment> {
        ensureEventExists(eventId)

        val sql = """
            SELECT c.id,
                   c.text,
                   c.event_id,
                   c.parent_comment_id,
                   c.comment_at,
                   c.user_id,
                   u.username
            FROM comment c
            LEFT JOIN app_user u ON u.id = c.user_id
            WHERE c.event_id = ?
            ORDER BY c.comment_at DESC, c.id DESC
            LIMIT ? OFFSET ?
        """.trimIndent()

        val records = dsl.resultQuery(sql, eventId, pageable.pageSize, pageable.offset).fetch()
        val items = records.map { record ->
            val commentAt = requireNotNull(record.get("comment_at", Timestamp::class.java)) {
                "comment_at is null for comment row"
            }
            ViewEventComment(
                id = requireNotNull(record.get("id", Long::class.java)) { "id is null for comment row" },
                text = requireNotNull(record.get("text", String::class.java)) { "text is null for comment row" },
                eventId = requireNotNull(record.get("event_id", Long::class.java)) { "event_id is null for comment row" },
                parentCommentId = record.get("parent_comment_id", Long::class.java),
                commentAt = commentAt.toInstant().toString(),
                userId = requireNotNull(record.get("user_id", Long::class.java)) { "user_id is null for comment row" },
                username = record.get("username", String::class.java),
            )
        }

        val total = dsl.fetchOne(
            """
            SELECT COUNT(*)
            FROM comment
            WHERE event_id = ?
            """.trimIndent(),
            eventId
        )?.get(0, Long::class.java) ?: 0L

        return PageImpl(items, pageable, total)
    }

    fun findReactionsByEventId(eventId: Long, pageable: Pageable): Page<ViewEventReaction> {
        ensureEventExists(eventId)

        val sql = """
            SELECT r.event_id,
                   r.user_id,
                   r.rating,
                   u.username
            FROM rating r
            LEFT JOIN app_user u ON u.id = r.user_id
            WHERE r.event_id = ?
            ORDER BY r.user_id
            LIMIT ? OFFSET ?
        """.trimIndent()

        val records = dsl.resultQuery(sql, eventId, pageable.pageSize, pageable.offset).fetch()
        val items = records.map { record ->
            ViewEventReaction(
                eventId = requireNotNull(record.get("event_id", Long::class.java)) { "event_id is null for reaction row" },
                userId = requireNotNull(record.get("user_id", Long::class.java)) { "user_id is null for reaction row" },
                username = record.get("username", String::class.java),
                rating = requireNotNull(record.get("rating", BigDecimal::class.java)) { "rating is null for reaction row" },
            )
        }

        val total = dsl.fetchOne(
            """
            SELECT COUNT(*)
            FROM rating
            WHERE event_id = ?
            """.trimIndent(),
            eventId
        )?.get(0, Long::class.java) ?: 0L

        return PageImpl(items, pageable, total)
    }

    private fun ensureEventExists(eventId: Long) {
        val exists = dsl.fetchOne("SELECT 1 FROM event WHERE id = ?", eventId) != null
        if (!exists) throw EventNotFoundException(eventId)
    }

    private fun findCategoriesByEventIds(eventIds: List<Long>): Map<Long, List<ViewEventCategory>> {
        if (eventIds.isEmpty()) return emptyMap()

        val placeholders = eventIds.joinToString(",") { "?" }
        val sql = """
            SELECT ce.event_id,
                   c.id AS category_id,
                   c.name AS category_name
            FROM category_event ce
            JOIN category c ON c.id = ce.category_id
            WHERE ce.event_id IN ($placeholders)
            ORDER BY ce.event_id, c.id
        """.trimIndent()

        return dsl.fetch(sql, *eventIds.toTypedArray())
            .groupBy(
                keySelector = { requireNotNull(it.get("event_id", Long::class.java)) { "event_id is null for category row" } },
                valueTransform = {
                    ViewEventCategory(
                        id = requireNotNull(it.get("category_id", Long::class.java)) { "category_id is null for category row" },
                        name = requireNotNull(it.get("category_name", String::class.java)) { "category_name is null for category row" },
                    )
                }
            )
    }

    private fun countValue(record: Record, alias: String): Int =
        (record.get(alias) as? Number)?.toInt() ?: 0
}
