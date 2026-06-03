package com.evmap.serverapp.features.search.infra.read

import com.evmap.serverapp.features.search.api.dto.ViewSearchCategory
import com.evmap.serverapp.features.search.api.dto.ViewSearchEvent
import com.evmap.serverapp.features.search.api.dto.ViewSearchLocation
import com.evmap.serverapp.features.search.api.dto.ViewSearchSuggestion
import com.evmap.serverapp.features.search.api.dto.ViewSearchUser
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class SearchReadRepository(
    private val dsl: DSLContext,
) {
    fun recordSearch(userId: Long, query: String, type: String) {
        dsl.execute(
            """
            INSERT INTO search_history(user_id, query, search_type, created_at)
            VALUES (?, ?, ?, CAST(? AS timestamptz))
            """.trimIndent(),
            userId,
            query,
            type,
            OffsetDateTime.now(ZoneOffset.UTC),
        )
    }

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
                similarity(u.username, ?) DESC,
                u.username ASC
            LIMIT ?
            """.trimIndent(),
            like,
            like,
            like,
            normalized,
            "$normalized%",
            "$normalized%",
            normalized,
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
        startsFrom: OffsetDateTime?,
        startsTo: OffsetDateTime?,
        onlyUpcoming: Boolean,
        sort: String,
        limit: Int,
    ): List<ViewSearchEvent> {
        val selectBindings = mutableListOf<Any>()
        val conditions = mutableListOf<String>()
        val conditionBindings = mutableListOf<Any>()
        val havings = mutableListOf<String>()
        val havingBindings = mutableListOf<Any>()
        val like = "%${query.trim()}%"
        val prefix = "${query.trim()}%"

        val distanceExpression = if (lat != null && lng != null) {
            selectBindings += lng
            selectBindings += lat
            """
            ST_Distance(
                COALESCE(l.point, ST_PointOnSurface(l.region))::geography,
                ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography
            )
            """.trimIndent()
        } else {
            "NULL::double precision"
        }

        conditions += """
            (
                to_tsvector('simple', COALESCE(e.name, '') || ' ' || COALESCE(e.description, '') || ' ' || COALESCE(l.name, ''))
                    @@ websearch_to_tsquery('simple', ?)
                OR e.name ILIKE ?
                OR e.description ILIKE ?
                OR l.name ILIKE ?
            )
        """.trimIndent()
        conditionBindings += query.trim()
        conditionBindings += like
        conditionBindings += like
        conditionBindings += like

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
            conditionBindings.addAll(categoryIds)
        }
        if (lat != null && lng != null && distanceKm != null) {
            conditions += """
                ST_DWithin(
                    COALESCE(l.point, ST_PointOnSurface(l.region))::geography,
                    ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography,
                    ?
                )
            """.trimIndent()
            conditionBindings += lng
            conditionBindings += lat
            conditionBindings += distanceKm * 1000.0
        }
        if (startsFrom != null) {
            conditions += "e.start_at >= CAST(? AS timestamptz)"
            conditionBindings += startsFrom
        }
        if (startsTo != null) {
            conditions += "e.start_at <= CAST(? AS timestamptz)"
            conditionBindings += startsTo
        }
        if (onlyUpcoming) {
            conditions += "e.start_at >= now()"
        }

        if (ratingFrom != null) {
            havings += "COALESCE(AVG(r.rating), 0) >= ?"
            havingBindings += BigDecimal.valueOf(ratingFrom)
        }
        if (ratingTo != null) {
            havings += "COALESCE(AVG(r.rating), 0) <= ?"
            havingBindings += BigDecimal.valueOf(ratingTo)
        }

        val havingSql = if (havings.isEmpty()) "" else "HAVING ${havings.joinToString("\n   AND ")}"
        val orderSql = orderBy(sort, includeDistance = lat != null && lng != null)
        val sql = """
            SELECT e.id,
                   e.name,
                   e.description,
                   e.start_at,
                   l.name AS location_name,
                   COALESCE(AVG(r.rating), 0)::double precision AS rating_avg,
                   COUNT(r.rating)::int AS ratings_count,
                   $distanceExpression AS distance_meters,
                   (CASE WHEN e.name ILIKE ? THEN 'name' END) AS matched_name,
                   (CASE WHEN e.description ILIKE ? THEN 'description' END) AS matched_description,
                   (CASE WHEN l.name ILIKE ? THEN 'location' END) AS matched_location
            FROM event e
            LEFT JOIN rating r ON r.event_id = e.id
            LEFT JOIN location l ON l.id = e.location_id
            WHERE ${conditions.joinToString("\n  AND ")}
            GROUP BY e.id, e.name, e.description, e.start_at, l.name, l.point, l.region
            $havingSql
            ORDER BY $orderSql
            LIMIT ?
        """.trimIndent()

        val matchBindings = listOf(like, like, like)
        val orderBindings = if (sort == "relevance") listOf(query.trim(), prefix, prefix) else emptyList()
        val records = dsl.fetch(
            sql,
            *(selectBindings + matchBindings + conditionBindings + havingBindings + orderBindings + limit).toTypedArray(),
        )
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
                matchedFields = listOfNotNull(
                    r.get("matched_name", String::class.java),
                    r.get("matched_description", String::class.java),
                    r.get("matched_location", String::class.java),
                ),
            )
        }
    }

    fun searchLocations(query: String, limit: Int): List<ViewSearchLocation> {
        val like = "%${query.trim()}%"
        return dsl.fetch(
            """
            SELECT l.id,
                   l.name,
                   l.category_id,
                   lc.name AS category_name
            FROM location l
            LEFT JOIN location_category lc ON lc.id = l.category_id
            WHERE l.name ILIKE ?
            ORDER BY similarity(l.name, ?) DESC, l.name ASC
            LIMIT ?
            """.trimIndent(),
            like,
            query.trim(),
            limit,
        ).map {
            ViewSearchLocation(
                id = requireNotNull(it.get("id", Long::class.java)) { "id is null for location row" },
                name = requireNotNull(it.get("name", String::class.java)) { "name is null for location row" },
                categoryId = requireNotNull(it.get("category_id", Long::class.java)) { "category_id is null for location row" },
                categoryName = it.get("category_name", String::class.java),
            )
        }
    }

    fun searchCategories(query: String, limit: Int): List<ViewSearchCategory> {
        val like = "%${query.trim()}%"
        return dsl.fetch(
            """
            SELECT id, name
            FROM category
            WHERE name ILIKE ?
            ORDER BY similarity(name, ?) DESC, name ASC
            LIMIT ?
            """.trimIndent(),
            like,
            query.trim(),
            limit,
        ).map {
            ViewSearchCategory(
                id = requireNotNull(it.get("id", Long::class.java)) { "id is null for category row" },
                name = requireNotNull(it.get("name", String::class.java)) { "name is null for category row" },
            )
        }
    }

    fun suggestions(query: String, limit: Int): List<ViewSearchSuggestion> {
        val events = searchSuggestionRows("events", "event", "name", query, limit)
        val locations = searchSuggestionRows("locations", "location", "name", query, limit)
        val categories = searchSuggestionRows("categories", "category", "name", query, limit)
        val users = dsl.fetch(
            """
            SELECT username AS value, username AS label
            FROM app_user
            WHERE username ILIKE ?
            ORDER BY similarity(username, ?) DESC, username ASC
            LIMIT ?
            """.trimIndent(),
            "%${query.trim()}%",
            query.trim(),
            limit,
        ).map {
            ViewSearchSuggestion("users", it.get("value", String::class.java)!!, "@${it.get("label", String::class.java)!!}")
        }
        return (events + locations + categories + users).take(limit)
    }

    fun popularSearches(limit: Int): List<ViewSearchSuggestion> =
        dsl.fetch(
            """
            SELECT search_type, query, COUNT(*) AS hits
            FROM search_history
            GROUP BY search_type, query
            ORDER BY hits DESC, MAX(created_at) DESC
            LIMIT ?
            """.trimIndent(),
            limit,
        ).map {
            val type = it.get("search_type", String::class.java) ?: "events"
            val query = it.get("query", String::class.java) ?: ""
            ViewSearchSuggestion(type = type, value = query, label = query)
        }

    private fun searchSuggestionRows(type: String, table: String, column: String, query: String, limit: Int): List<ViewSearchSuggestion> =
        dsl.fetch(
            """
            SELECT $column AS value
            FROM $table
            WHERE $column ILIKE ?
            ORDER BY similarity($column, ?) DESC, $column ASC
            LIMIT ?
            """.trimIndent(),
            "%${query.trim()}%",
            query.trim(),
            limit,
        ).map {
            val value = it.get("value", String::class.java)!!
            ViewSearchSuggestion(type = type, value = value, label = value)
        }

    private fun orderBy(sort: String, includeDistance: Boolean): String {
        val relevance = """
            CASE
                WHEN LOWER(e.name) = LOWER(?) THEN 300
                WHEN LOWER(e.name) LIKE LOWER(?) THEN 200
                WHEN LOWER(l.name) LIKE LOWER(?) THEN 100
                ELSE 0
            END DESC
        """.trimIndent()
        return when (sort) {
            "rating" -> "rating_avg DESC, ratings_count DESC, e.start_at DESC"
            "distance" -> if (includeDistance) "distance_meters ASC NULLS LAST, rating_avg DESC, e.start_at DESC" else "e.start_at DESC"
            "date" -> "e.start_at DESC, rating_avg DESC"
            else -> "$relevance, rating_avg DESC, e.start_at DESC"
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
