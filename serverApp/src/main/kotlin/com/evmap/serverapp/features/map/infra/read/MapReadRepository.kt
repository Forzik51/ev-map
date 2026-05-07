package com.evmap.serverapp.features.map.infra.read

import com.evmap.serverapp.features.map.api.dto.ViewMapCategory
import com.evmap.serverapp.features.map.api.dto.ViewMapEventItem
import com.evmap.serverapp.features.map.api.dto.ViewMapPoint
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.math.BigDecimal

@Repository
class MapReadRepository(
    private val dsl: DSLContext,
) {
    fun findNearby(
        lat: Double,
        lng: Double,
        radiusMeters: Double,
        categoryIds: List<Long>,
        limit: Int,
    ): List<ViewMapEventItem> {
        val bindings = mutableListOf<Any>()
        val conditions = mutableListOf<String>()

        conditions += """
            ST_DWithin(
                COALESCE(l.point, ST_PointOnSurface(l.region))::geography,
                ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography,
                ?
            )
        """.trimIndent()
        bindings += lng
        bindings += lat
        bindings += radiusMeters

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

        val sql = """
            SELECT e.id AS event_id,
                   e.name AS event_name,
                   l.name AS location_name,
                   CASE
                       WHEN l.region IS NOT NULL THEN 'REGION'
                       WHEN l.name IS NOT NULL AND BTRIM(l.name) <> '' THEN 'NAMED_POINT'
                       ELSE 'POINT'
                   END AS geometry_type,
                   ST_Y(COALESCE(l.point, ST_PointOnSurface(l.region))) AS lat,
                   ST_X(COALESCE(l.point, ST_PointOnSurface(l.region))) AS lng,
                   ST_AsGeoJSON(l.region) AS region_geojson,
                   ST_Distance(
                       COALESCE(l.point, ST_PointOnSurface(l.region))::geography,
                       ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography
                   ) AS distance_meters,
                   COALESCE(AVG(r.rating), 0)::double precision AS rating_avg,
                   COUNT(r.rating)::int AS ratings_count
            FROM event e
            JOIN rating r ON r.event_id = e.id
            JOIN location l ON l.id = e.location_id
            WHERE ${conditions.joinToString("\n  AND ")}
            GROUP BY e.id, e.name, l.name, l.point, l.region
            HAVING COUNT(r.rating) > 0
            ORDER BY distance_meters ASC, rating_avg DESC, e.start_at DESC
            LIMIT ?
        """.trimIndent()

        bindings += lng
        bindings += lat
        bindings += limit
        return toViewItems(dsl.fetch(sql, *bindings.toTypedArray()))
    }

    fun search(
        query: String,
        lat: Double?,
        lng: Double?,
        distanceKm: Double?,
        ratingFrom: Double?,
        ratingTo: Double?,
        categoryIds: List<Long>,
        limit: Int,
    ): List<ViewMapEventItem> {
        val bindings = mutableListOf<Any>()
        val conditions = mutableListOf<String>()
        val havings = mutableListOf<String>()
        val distanceMeters = distanceKm?.times(1000.0)
        val like = "%${query.trim()}%"
        val prefix = "${query.trim()}%"

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

        val distanceExpr = if (lat != null && lng != null) {
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
            SELECT e.id AS event_id,
                   e.name AS event_name,
                   l.name AS location_name,
                   CASE
                       WHEN l.region IS NOT NULL THEN 'REGION'
                       WHEN l.name IS NOT NULL AND BTRIM(l.name) <> '' THEN 'NAMED_POINT'
                       ELSE 'POINT'
                   END AS geometry_type,
                   ST_Y(COALESCE(l.point, ST_PointOnSurface(l.region))) AS lat,
                   ST_X(COALESCE(l.point, ST_PointOnSurface(l.region))) AS lng,
                   ST_AsGeoJSON(l.region) AS region_geojson,
                   $distanceExpr AS distance_meters,
                   COALESCE(AVG(r.rating), 0)::double precision AS rating_avg,
                   COUNT(r.rating)::int AS ratings_count
            FROM event e
            JOIN rating r ON r.event_id = e.id
            JOIN location l ON l.id = e.location_id
            WHERE ${conditions.joinToString("\n  AND ")}
            GROUP BY e.id, e.name, l.name, l.point, l.region
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
        return toViewItems(dsl.fetch(sql, *bindings.toTypedArray()))
    }

    private fun toViewItems(records: org.jooq.Result<org.jooq.Record>): List<ViewMapEventItem> {
        val eventIds = records.mapNotNull { it.get("event_id", Long::class.java) }
        val categoriesByEventId = findCategoriesByEventIds(eventIds)

        return records.map { r ->
            val eventId = requireNotNull(r.get("event_id", Long::class.java)) { "event_id is null for map row" }
            val lat = requireNotNull((r.get("lat") as? Number)?.toDouble()) { "lat is null for map row" }
            val lng = requireNotNull((r.get("lng") as? Number)?.toDouble()) { "lng is null for map row" }
            ViewMapEventItem(
                eventId = eventId,
                eventName = requireNotNull(r.get("event_name", String::class.java)) { "event_name is null for map row" },
                locationName = r.get("location_name", String::class.java),
                geometryType = requireNotNull(r.get("geometry_type", String::class.java)) { "geometry_type is null for map row" },
                point = ViewMapPoint(lat = lat, lng = lng),
                regionGeoJson = r.get("region_geojson", String::class.java),
                distanceMeters = (r.get("distance_meters") as? Number)?.toDouble(),
                ratingAvg = (r.get("rating_avg") as? Number)?.toDouble() ?: 0.0,
                ratingsCount = (r.get("ratings_count") as? Number)?.toInt() ?: 0,
                categories = categoriesByEventId[eventId].orEmpty(),
            )
        }
    }

    private fun findCategoriesByEventIds(eventIds: List<Long>): Map<Long, List<ViewMapCategory>> {
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
                    ViewMapCategory(
                        id = requireNotNull(it.get("category_id", Long::class.java)) { "category_id is null for category row" },
                        name = requireNotNull(it.get("category_name", String::class.java)) { "category_name is null for category row" },
                    )
                },
            )
    }
}
