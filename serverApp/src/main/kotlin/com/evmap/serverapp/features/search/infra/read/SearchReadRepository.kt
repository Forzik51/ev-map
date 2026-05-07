package com.evmap.serverapp.features.search.infra.read

import com.evmap.serverapp.features.search.api.dto.ViewSearchCategory
import com.evmap.serverapp.features.search.api.dto.ViewSearchEvent
import com.evmap.serverapp.features.search.api.dto.ViewSearchUser
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.sql.Timestamp

@Repository
class SearchReadRepository(
    private val dsl: DSLContext,
) {
    fun searchUsers(query: String, limit: Int): List<ViewSearchUser> {
        val normalized = query.trim().removePrefix("@")
        val like = "%$normalized%"

        return dsl.fetch(
            """
            SELECT u.id,
                   u.username,
                   u.name,
                   u.surname,
                   u.page_description
            FROM app_user u
            WHERE u.username ILIKE ?
               OR u.name ILIKE ?
               OR COALESCE(u.surname, '') ILIKE ?
            ORDER BY
                CASE
                    WHEN LOWER(u.username) = LOWER(?) THEN 300
                    WHEN LOWER(u.username) LIKE LOWER(?) THEN 200
                    WHEN LOWER(u.name) LIKE LOWER(?) THEN 100
                    ELSE 0
                END DESC,
                u.username ASC
            LIMIT ?
            """.trimIndent(),
            like,
            like,
            like,
            normalized,
            "$normalized%",
            "$normalized%",
            limit,
        ).map {
            ViewSearchUser(
                id = requireNotNull(it.get("id", Long::class.java)) { "id is null for user row" },
                username = requireNotNull(it.get("username", String::class.java)) { "username is null for user row" },
                name = requireNotNull(it.get("name", String::class.java)) { "name is null for user row" },
                surname = it.get("surname", String::class.java),
                pageDescription = requireNotNull(it.get("page_description", String::class.java)) { "page_description is null for user row" },
            )
        }
    }

    fun searchEvents(
        query: String,
        lat: Double?,
        lng: Double?,
        distanceKm: Double?,
        ratingFrom: Double?,
        ratingTo: Double?,
        categoryIds: List<Long>,
        limit: Int,
    ): List<ViewSearchEvent> {
        val bindings = mutableListOf<Any>()
        val conditions = mutableListOf<String>()
        val havings = mutableListOf<String>()
        val distanceMeters = distanceKm?.times(1000.0)
        val like = "%${query.trim()}%"
        val prefix = "${query.trim()}%"

        val distanceExpression = if (lat != null && lng != null) {
            """
            ST_Distance(
                COALESCE(l.point, ST_PointOnSurface(l.region))::geography,
                ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography
            )
            """.trimIndent().also {
                bindings += lng
                bindings += lat
            }
        } else {
            "NULL::double precision"
        }

        conditions += "(e.name ILIKE ? OR e.description ILIKE ? OR l.name ILIKE ?)"
        bindings += like
        bindings += like
        bindings += like

        if (categoryIds.isNotEmpty()) {
            val placeholders = categoryIds.joinToString(",") { "?" }
            conditions += """
                EXISTS (
                    SELECT 1
                    FROM category_event ce_filter
                    WHERE ce_filter.event_id = e.id
                      AND ce_filter.category_id IN ($placeholders)
                )
            """.trimIndent()
            bindings.addAll(categoryIds)
        }

        if (lat != null && lng != null && distanceMeters != null) {
            conditions += """
                ST_DWithin(
                    COALESCE(l.point, ST_PointOnSurface(l.region))::geography,
                    ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography,
                    ?
                )
            """.trimIndent()
            bindings += lng
            bindings += lat
            bindings += distanceMeters
        }

        havings += "COUNT(r.rating) > 0"

        if (ratingFrom != null) {
            havings += "AVG(r.rating) >= ?"
            bindings += BigDecimal.valueOf(ratingFrom)
        }
        if (ratingTo != null) {
            havings += "AVG(r.rating) <= ?"
            bindings += BigDecimal.valueOf(ratingTo)
        }

        val sql = """
            SELECT e.id,
                   e.name,
                   e.description,
                   e.start_at,
                   l.name AS location_name,
                   COALESCE(AVG(r.rating), 0)::double precision AS rating_avg,
                   COUNT(r.rating)::int AS ratings_count,
                   $distanceExpression AS distance_meters
            FROM event e
            JOIN rating r ON r.event_id = e.id
            LEFT JOIN location l ON l.id = e.location_id
            WHERE ${conditions.joinToString("\n  AND ")}
            GROUP BY e.id, e.name, e.description, e.start_at, l.name, l.point, l.region
            HAVING ${havings.joinToString("\n   AND ")}
            ORDER BY
                CASE
                    WHEN LOWER(e.name) = LOWER(?) THEN 300
                    WHEN LOWER(e.name) LIKE LOWER(?) THEN 200
                    WHEN LOWER(l.name) LIKE LOWER(?) THEN 100
                    ELSE 0
                END DESC,
                rating_avg DESC,
                e.start_at DESC
            LIMIT ?
        """.trimIndent()

        bindings += query.trim()
        bindings += prefix
        bindings += prefix
        bindings += limit

        val records = dsl.fetch(sql, *bindings.toTypedArray())
        val eventIds = records.mapNotNull { it.get("id", Long::class.java) }
        val categoriesByEventId = findCategoriesByEventIds(eventIds)

        return records.map { r ->
            val eventId = requireNotNull(r.get("id", Long::class.java)) { "id is null for event search row" }
            val startsAt = requireNotNull(r.get("start_at", Timestamp::class.java)) { "start_at is null for event search row" }
            ViewSearchEvent(
                id = eventId,
                name = requireNotNull(r.get("name", String::class.java)) { "name is null for event search row" },
                description = requireNotNull(r.get("description", String::class.java)) { "description is null for event search row" },
                locationName = r.get("location_name", String::class.java),
                startsAt = startsAt.toInstant().toString(),
                ratingAvg = (r.get("rating_avg") as? Number)?.toDouble() ?: 0.0,
                ratingsCount = (r.get("ratings_count") as? Number)?.toInt() ?: 0,
                distanceMeters = (r.get("distance_meters") as? Number)?.toDouble(),
                categories = categoriesByEventId[eventId].orEmpty(),
            )
        }
    }

    private fun findCategoriesByEventIds(eventIds: List<Long>): Map<Long, List<ViewSearchCategory>> {
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
                    ViewSearchCategory(
                        id = requireNotNull(it.get("category_id", Long::class.java)) { "category_id is null for category row" },
                        name = requireNotNull(it.get("category_name", String::class.java)) { "category_name is null for category row" },
                    )
                },
            )
    }
}
