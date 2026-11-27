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
                   e.start_at, e.created_at, e.date_mode, e.starts_on, e.ends_on, e.time_zone,
                   ARRAY(SELECT t.name FROM event_tag et JOIN tag t ON t.id=et.tag_id WHERE et.event_id=e.id ORDER BY t.name) AS tags,
                   e.end_at,
                   e.user_id,
                   e.location_id,
                   l.name AS location_name,
                   ph.image_path AS image_path,
                   COALESCE(rp.reposts_count, 0) AS reposts_count,
                   COALESCE(rt.ratings_count, 0) AS ratings_count,
                   rt.rating_avg,
                   COALESCE(cm.comments_count, 0) AS comments_count
            FROM event e
            LEFT JOIN location l ON l.id = e.location_id
            LEFT JOIN LATERAL (
                SELECT p.image_path
                FROM photo p
                WHERE p.event_id = e.id
                ORDER BY p.sort_order, p.id
                LIMIT 1
            ) ph ON TRUE
            LEFT JOIN (
                SELECT event_id, COUNT(*) AS reposts_count
                FROM repost
                GROUP BY event_id
            ) rp ON rp.event_id = e.id
            LEFT JOIN (
                SELECT event_id, COUNT(*) AS ratings_count, AVG(rating) AS rating_avg
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
            val endAt = r.get("end_at", Timestamp::class.java)
            val categoriesByEventId = findCategoriesByEventIds(listOf(id))

            ViewEvent(
                id = requireNotNull(r.get("id", Long::class.java)) { "id is null for id=$id" },
                name = requireNotNull(r.get("name", String::class.java)) { "name is null for id=$id" },
                description = requireNotNull(r.get("description", String::class.java)) { "description is null for id=$id" },
                startsAt = startAt?.toInstant()?.toString(),
                endsAt = endAt?.toInstant()?.toString(),
                locationName = r.get("location_name", String::class.java),
                imagePath = r.get("image_path", String::class.java),
                userId = requireNotNull(r.get("user_id", Long::class.java)) { "user_id is null for id=$id" },
                repostsCount = countValue(r, "reposts_count"),
                ratingsCount = countValue(r, "ratings_count"),
                commentsCount = countValue(r, "comments_count"),
                categories = categoriesByEventId[id].orEmpty(),
                locationId = requireNotNull(r.get("location_id", Long::class.java)),
                ratingAvg = (r.get("rating_avg") as? Number)?.toDouble(),
                createdAt = r.get("created_at", Timestamp::class.java)?.toInstant()?.toString(),
                dateMode = r.get("date_mode", String::class.java) ?: "DATETIME",
                startsOn = r.get("starts_on")?.toString(), endsOn = r.get("ends_on")?.toString(),
                timeZone = r.get("time_zone", String::class.java) ?: "UTC",
                tags = (r.get("tags") as? Array<*>)?.map { it.toString() }.orEmpty(),
            )
        } ?: throw EventNotFoundException(id)

    private val eventViewSelect = """
            SELECT e.id,
                   e.name,
                   e.description,
                   e.start_at, e.created_at, e.date_mode, e.starts_on, e.ends_on, e.time_zone,
                   ARRAY(SELECT t.name FROM event_tag et JOIN tag t ON t.id=et.tag_id WHERE et.event_id=e.id ORDER BY t.name) AS tags,
                   e.end_at,
                   e.user_id,
                   e.location_id,
                   l.name AS location_name,
                   ph.image_path AS image_path,
                   COALESCE(rp.reposts_count, 0) AS reposts_count,
                   COALESCE(rt.ratings_count, 0) AS ratings_count,
                   rt.rating_avg,
                   COALESCE(cm.comments_count, 0) AS comments_count
            FROM event e
            LEFT JOIN location l ON l.id = e.location_id
            LEFT JOIN LATERAL (
                SELECT p.image_path
                FROM photo p
                WHERE p.event_id = e.id
                ORDER BY p.sort_order, p.id
                LIMIT 1
            ) ph ON TRUE
            LEFT JOIN (
                SELECT event_id, COUNT(*) AS reposts_count
                FROM repost
                GROUP BY event_id
            ) rp ON rp.event_id = e.id
            LEFT JOIN (
                SELECT event_id, COUNT(*) AS ratings_count, AVG(rating) AS rating_avg
                FROM rating
                GROUP BY event_id
            ) rt ON rt.event_id = e.id
            LEFT JOIN (
                SELECT event_id, COUNT(*) AS comments_count
                FROM comment
                GROUP BY event_id
            ) cm ON cm.event_id = e.id
    """.trimIndent()

    fun findAllViews(pageable: Pageable, userId: Long? = null): Page<ViewEvent> {
        val where = if (userId == null) "" else "WHERE e.user_id = ?"
        val filter = listOfNotNull(userId).toTypedArray()
        val items = mapEventViews(dsl.fetch(
            "$eventViewSelect $where ORDER BY e.created_at DESC, e.id DESC LIMIT ? OFFSET ?",
            *filter,
            pageable.pageSize,
            pageable.offset,
        ))
        val total = dsl.fetchOne("SELECT COUNT(*) FROM event e $where", *filter)?.get(0, Long::class.java) ?: 0L
        return PageImpl(items, pageable, total)
    }

    fun findSavedListViews(listId: Long): List<ViewEvent> = mapEventViews(dsl.fetch(
        """
        $eventViewSelect
        WHERE EXISTS (SELECT 1 FROM event_list el WHERE el.favourite_list_id = ? AND el.event_id = e.id)
        ORDER BY e.id
        """.trimIndent(),
        listId,
    ))

    private fun mapEventViews(recs: List<Record>): List<ViewEvent> {
        val eventIds = recs.mapNotNull { it.get("id", Long::class.java) }
        val categoriesByEventId = findCategoriesByEventIds(eventIds)

        return recs.map {
            val eventId = requireNotNull(it.get("id", Long::class.java)) { "id is null for event row" }
            ViewEvent(
                id = eventId,
                name = requireNotNull(it.get("name", String::class.java)) { "name is null for event row" },
                description = requireNotNull(it.get("description", String::class.java)) { "description is null for event row" },
                startsAt = it.get("start_at", Timestamp::class.java)?.toInstant()?.toString(),
                endsAt = it.get("end_at", Timestamp::class.java)?.toInstant()?.toString(),
                locationName = it.get("location_name", String::class.java),
                imagePath = it.get("image_path", String::class.java),
                userId = requireNotNull(it.get("user_id", Long::class.java)) { "user_id is null for event row" },
                repostsCount = countValue(it, "reposts_count"),
                ratingsCount = countValue(it, "ratings_count"),
                commentsCount = countValue(it, "comments_count"),
                categories = categoriesByEventId[eventId].orEmpty(),
                locationId = requireNotNull(it.get("location_id", Long::class.java)),
                ratingAvg = (it.get("rating_avg") as? Number)?.toDouble(),
                createdAt = it.get("created_at", Timestamp::class.java)?.toInstant()?.toString(),
                dateMode = it.get("date_mode", String::class.java) ?: "DATETIME",
                startsOn = it.get("starts_on")?.toString(), endsOn = it.get("ends_on")?.toString(),
                timeZone = it.get("time_zone", String::class.java) ?: "UTC",
                tags = (it.get("tags") as? Array<*>)?.map { it.toString() }.orEmpty(),
            )
        }

    }

    fun findPersonalizedViews(userId: Long, lat: Double?, lng: Double?, pageable: Pageable): Page<ViewEvent> {
        val distanceSelect = if (lat != null && lng != null) {
            """
            ST_Distance(
                l.shape::geography,
                ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography
            )
            """.trimIndent()
        } else {
            "NULL::double precision"
        }
        val selectBindings = if (lat != null && lng != null) listOf(lng, lat) else emptyList()
        val sql = """
            SELECT e.id,
                   e.name,
                   e.description,
                   e.start_at, e.created_at, e.date_mode, e.starts_on, e.ends_on, e.time_zone,
                   ARRAY(SELECT t.name FROM event_tag et JOIN tag t ON t.id=et.tag_id WHERE et.event_id=e.id ORDER BY t.name) AS tags,
                   e.end_at,
                   e.user_id,
                   e.location_id,
                   l.name AS location_name,
                   ph.image_path AS image_path,
                   COALESCE(rp.reposts_count, 0) AS reposts_count,
                   COALESCE(rt.ratings_count, 0) AS ratings_count,
                   rt.rating_avg,
                   COALESCE(cm.comments_count, 0) AS comments_count,
                   $distanceSelect AS distance_meters,
                   (
                       CASE WHEN f.following_id IS NOT NULL THEN 80 ELSE 0 END
                       + CASE WHEN e.start_at >= now() THEN 30 ELSE 0 END
                       + CASE WHEN pref.event_id IS NOT NULL THEN 40 ELSE 0 END
                       + LEAST(COALESCE(rt.ratings_count, 0), 20)
                   ) AS personalization_score
            FROM event e
            LEFT JOIN location l ON l.id = e.location_id
            LEFT JOIN follow f ON f.follower_id = ? AND f.following_id = e.user_id
            LEFT JOIN LATERAL (
                SELECT p.image_path
                FROM photo p
                WHERE p.event_id = e.id
                ORDER BY p.sort_order, p.id
                LIMIT 1
            ) ph ON TRUE
            LEFT JOIN (
                SELECT event_id, COUNT(*) AS reposts_count
                FROM repost
                GROUP BY event_id
            ) rp ON rp.event_id = e.id
            LEFT JOIN (
                SELECT event_id, COUNT(*) AS ratings_count, AVG(rating) AS rating_avg
                FROM rating
                GROUP BY event_id
            ) rt ON rt.event_id = e.id
            LEFT JOIN (
                SELECT event_id, COUNT(*) AS comments_count
                FROM comment
                GROUP BY event_id
            ) cm ON cm.event_id = e.id
            LEFT JOIN (
                SELECT DISTINCT ce.event_id
                FROM category_event ce
                WHERE ce.category_id IN (
                    SELECT ce2.category_id
                    FROM rating r2
                    JOIN category_event ce2 ON ce2.event_id = r2.event_id
                    WHERE r2.user_id = ?
                    UNION
                    SELECT ce3.category_id
                    FROM comment c3
                    JOIN category_event ce3 ON ce3.event_id = c3.event_id
                    WHERE c3.user_id = ?
                )
            ) pref ON pref.event_id = e.id
            ORDER BY personalization_score DESC,
                     distance_meters ASC NULLS LAST,
                     e.created_at DESC,
                     e.id DESC
            LIMIT ? OFFSET ?
        """.trimIndent()

        val recs = dsl.resultQuery(
            sql,
            *(selectBindings + listOf(userId, userId, userId, pageable.pageSize, pageable.offset)).toTypedArray(),
        ).fetch()

        val eventIds = recs.mapNotNull { it.get("id", Long::class.java) }
        val categoriesByEventId = findCategoriesByEventIds(eventIds)
        val items = recs.map {
            val eventId = requireNotNull(it.get("id", Long::class.java)) { "id is null for event row" }
            ViewEvent(
                id = eventId,
                name = requireNotNull(it.get("name", String::class.java)) { "name is null for event row" },
                description = requireNotNull(it.get("description", String::class.java)) { "description is null for event row" },
                startsAt = it.get("start_at", Timestamp::class.java)?.toInstant()?.toString(),
                endsAt = it.get("end_at", Timestamp::class.java)?.toInstant()?.toString(),
                locationName = it.get("location_name", String::class.java),
                imagePath = it.get("image_path", String::class.java),
                userId = requireNotNull(it.get("user_id", Long::class.java)) { "user_id is null for event row" },
                repostsCount = countValue(it, "reposts_count"),
                ratingsCount = countValue(it, "ratings_count"),
                commentsCount = countValue(it, "comments_count"),
                categories = categoriesByEventId[eventId].orEmpty(),
                locationId = requireNotNull(it.get("location_id", Long::class.java)),
                ratingAvg = (it.get("rating_avg") as? Number)?.toDouble(),
                createdAt = it.get("created_at", Timestamp::class.java)?.toInstant()?.toString(),
                dateMode = it.get("date_mode", String::class.java) ?: "DATETIME",
                startsOn = it.get("starts_on")?.toString(), endsOn = it.get("ends_on")?.toString(),
                timeZone = it.get("time_zone", String::class.java) ?: "UTC",
                tags = (it.get("tags") as? Array<*>)?.map { it.toString() }.orEmpty(),
            )
        }

        val total = dsl.fetchOne("SELECT COUNT(*) FROM event")?.get(0, Long::class.java) ?: 0L
        return PageImpl(items, pageable, total)
    }

    fun findUserRating(eventId: Long, userId: Long): Double? =
        (dsl.fetchOne("SELECT rating FROM rating WHERE event_id = ? AND user_id = ?", eventId, userId)?.get(0) as? Number)
            ?.toDouble()

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
                parentCommentId = record.get("parent_comment_id", Long::class.javaObjectType),
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
