package com.evmap.serverapp.features.map.infra.read

import com.evmap.serverapp.features.event.application.EventNotFoundException
import com.evmap.serverapp.features.map.api.dto.ViewMapCategory
import com.evmap.serverapp.features.map.api.dto.ViewMapCluster
import com.evmap.serverapp.features.map.api.dto.ViewMapEventDetails
import com.evmap.serverapp.features.map.api.dto.ViewMapEventItem
import com.evmap.serverapp.features.map.api.dto.ViewMapPoint
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.OffsetDateTime

@Repository
class MapReadRepository(
    private val dsl: DSLContext,
) {
    fun findNearby(
        lat: Double,
        lng: Double,
        radiusMeters: Double,
        categoryIds: List<Long>,
        locationCategoryIds: List<Long>,
        startsFrom: OffsetDateTime?,
        startsTo: OffsetDateTime?,
        onlyActive: Boolean,
        sort: String,
        limit: Int,
        tags: List<String> = emptyList(),
    ): List<ViewMapEventItem> {
        val selectBindings = mutableListOf<Any>(lng, lat)
        val conditions = mutableListOf<String>()
        val conditionBindings = mutableListOf<Any>()

        conditions += """
            ST_DWithin(
                l.shape::geography,
                ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography,
                ?
            )
        """.trimIndent()
        conditionBindings += lng
        conditionBindings += lat
        conditionBindings += radiusMeters
        addCommonFilters(conditions, conditionBindings, categoryIds, locationCategoryIds, startsFrom, startsTo, onlyActive, tags)

        val sql = eventItemSql(
            distanceExpression = """
                ST_Distance(
                    l.shape::geography,
                    ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography
                )
            """.trimIndent(),
            conditions = conditions,
            having = emptyList(),
            orderBy = orderBy(sort, includeDistance = true, includeRelevance = false),
        )

        return toViewItems(dsl.fetch(sql, *(selectBindings + conditionBindings + limit).toTypedArray()))
    }

    fun findBounds(
        minLat: Double,
        minLng: Double,
        maxLat: Double,
        maxLng: Double,
        categoryIds: List<Long>,
        locationCategoryIds: List<Long>,
        startsFrom: OffsetDateTime?,
        startsTo: OffsetDateTime?,
        onlyActive: Boolean,
        sort: String,
        limit: Int,
        tags: List<String> = emptyList(),
    ): List<ViewMapEventItem> {
        val conditions = mutableListOf(boundsCondition())
        val bindings = mutableListOf<Any>(minLng, minLat, maxLng, maxLat, minLng, minLat, maxLng, maxLat)
        addCommonFilters(conditions, bindings, categoryIds, locationCategoryIds, startsFrom, startsTo, onlyActive, tags)

        val sql = eventItemSql(
            distanceExpression = "NULL::double precision",
            conditions = conditions,
            having = emptyList(),
            orderBy = orderBy(sort, includeDistance = false, includeRelevance = false),
        )

        return toViewItems(dsl.fetch(sql, *(bindings + limit).toTypedArray()))
    }

    fun search(
        query: String,
        lat: Double?,
        lng: Double?,
        distanceKm: Double?,
        ratingFrom: Double?,
        ratingTo: Double?,
        categoryIds: List<Long>,
        locationCategoryIds: List<Long>,
        startsFrom: OffsetDateTime?,
        startsTo: OffsetDateTime?,
        onlyActive: Boolean,
        sort: String,
        limit: Int,
        tags: List<String> = emptyList(),
    ): List<ViewMapEventItem> {
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
                l.shape::geography,
                ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography
            )
            """.trimIndent()
        } else {
            "NULL::double precision"
        }

        conditions += "(e.name ILIKE ? OR e.description ILIKE ? OR l.name ILIKE ?)"
        conditionBindings += like
        conditionBindings += like
        conditionBindings += like
        addCommonFilters(conditions, conditionBindings, categoryIds, locationCategoryIds, startsFrom, startsTo, onlyActive, tags)

        if (lat != null && lng != null && distanceKm != null) {
            conditions += """
                ST_DWithin(
                    l.shape::geography,
                    ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography,
                    ?
                )
            """.trimIndent()
            conditionBindings += lng
            conditionBindings += lat
            conditionBindings += distanceKm * 1000.0
        }

        if (ratingFrom != null) {
            havings += "COALESCE(AVG(r.rating), 0) >= ?"
            havingBindings += BigDecimal.valueOf(ratingFrom)
        }
        if (ratingTo != null) {
            havings += "COALESCE(AVG(r.rating), 0) <= ?"
            havingBindings += BigDecimal.valueOf(ratingTo)
        }

        val relevanceBindings = if (sort == "relevance") listOf(query.trim(), prefix, prefix) else emptyList()
        val sql = eventItemSql(
            distanceExpression = distanceExpression,
            conditions = conditions,
            having = havings,
            orderBy = orderBy(sort, includeDistance = lat != null && lng != null, includeRelevance = true),
        )

        return toViewItems(
            dsl.fetch(sql, *(selectBindings + conditionBindings + havingBindings + relevanceBindings + limit).toTypedArray()),
        )
    }

    fun findClusters(
        minLat: Double,
        minLng: Double,
        maxLat: Double,
        maxLng: Double,
        zoom: Int,
        categoryIds: List<Long>,
        locationCategoryIds: List<Long>,
        startsFrom: OffsetDateTime?,
        startsTo: OffsetDateTime?,
        onlyActive: Boolean,
        tags: List<String> = emptyList(),
    ): List<ViewMapCluster> {
        val cellSize = when {
            zoom <= 5 -> 2.0
            zoom <= 8 -> 0.5
            zoom <= 11 -> 0.1
            zoom <= 14 -> 0.03
            else -> 0.01
        }
        val conditions = mutableListOf(boundsCondition())
        val bindings = mutableListOf<Any>(minLng, minLat, maxLng, maxLat, minLng, minLat, maxLng, maxLat)
        addCommonFilters(conditions, bindings, categoryIds, locationCategoryIds, startsFrom, startsTo, onlyActive, tags)

        val sql = """
            SELECT COUNT(*)::int AS count,
                   AVG(ST_Y(COALESCE(l.point, ST_PointOnSurface(l.shape)))) AS lat,
                   AVG(ST_X(COALESCE(l.point, ST_PointOnSurface(l.shape)))) AS lng
            FROM event e
            JOIN location l ON l.id = e.location_id
            WHERE ${conditions.joinToString("\n  AND ")}
            GROUP BY FLOOR(ST_X(COALESCE(l.point, ST_PointOnSurface(l.shape))) / ?),
                     FLOOR(ST_Y(COALESCE(l.point, ST_PointOnSurface(l.shape))) / ?)
            ORDER BY count DESC
        """.trimIndent()

        return dsl.fetch(sql, *(bindings + cellSize + cellSize).toTypedArray()).map {
            ViewMapCluster(
                count = (it.get("count") as? Number)?.toInt() ?: 0,
                point = ViewMapPoint(
                    lat = requireNotNull((it.get("lat") as? Number)?.toDouble()) { "lat is null for cluster row" },
                    lng = requireNotNull((it.get("lng") as? Number)?.toDouble()) { "lng is null for cluster row" },
                ),
            )
        }
    }

    fun findDetails(eventId: Long): ViewMapEventDetails {
        val record = dsl.fetchOne(
            """
            SELECT e.id AS event_id,
                   e.name AS event_name,
                   e.description,
                   e.start_at, e.date_mode, e.starts_on, e.ends_on, e.time_zone,
                   e.end_at,
                   l.name AS location_name,
                   CASE WHEN GeometryType(l.shape)='POINT' THEN 'POINT' WHEN GeometryType(l.shape) IN ('LINESTRING','MULTILINESTRING') THEN 'LINE' ELSE 'REGION' END AS geometry_type,
                   ST_Y(COALESCE(l.point, ST_PointOnSurface(l.shape))) AS lat,
                   ST_X(COALESCE(l.point, ST_PointOnSurface(l.shape))) AS lng,
                   ST_AsGeoJSON(CASE WHEN GeometryType(l.shape)<>'POINT' THEN l.shape END) AS region_geojson,
                   ph.image_path AS image_path,
                   COALESCE(AVG(r.rating), 0)::double precision AS rating_avg,
                   COUNT(r.rating)::int AS ratings_count,
                   COALESCE(rp.reposts_count, 0)::int AS reposts_count,
                   COALESCE(cm.comments_count, 0)::int AS comments_count
            FROM event e
            JOIN location l ON l.id = e.location_id
            LEFT JOIN rating r ON r.event_id = e.id
            LEFT JOIN LATERAL (
                SELECT p.image_path
                FROM photo p
                WHERE p.event_id = e.id
                ORDER BY p.sort_order,p.id
                LIMIT 1
            ) ph ON TRUE
            LEFT JOIN (
                SELECT event_id, COUNT(*) AS reposts_count
                FROM repost
                GROUP BY event_id
            ) rp ON rp.event_id = e.id
            LEFT JOIN (
                SELECT event_id, COUNT(*) AS comments_count
                FROM comment
                GROUP BY event_id
            ) cm ON cm.event_id = e.id
            WHERE e.id = ?
            GROUP BY e.id, e.name, e.description, e.start_at, e.end_at, l.name, l.point, l.region, l.shape,
                     ph.image_path, rp.reposts_count, cm.comments_count
            """.trimIndent(),
            eventId,
        ) ?: throw EventNotFoundException(eventId)

        val categories = findCategoriesByEventIds(listOf(eventId))[eventId].orEmpty()
        val startsAt = record.get("start_at", Timestamp::class.java)
        return ViewMapEventDetails(
            eventId = eventId,
            eventName = requireNotNull(record.get("event_name", String::class.java)) { "event_name is null for map details row" },
            description = requireNotNull(record.get("description", String::class.java)) { "description is null for map details row" },
            startsAt = startsAt?.toInstant()?.toString(),
            dateMode=record.get("date_mode",String::class.java),startsOn=record.get("starts_on")?.toString(),endsOn=record.get("ends_on")?.toString(),timeZone=record.get("time_zone",String::class.java),
            endsAt = record.get("end_at", Timestamp::class.java)?.toInstant()?.toString(),
            locationName = record.get("location_name", String::class.java),
            geometryType = requireNotNull(record.get("geometry_type", String::class.java)) { "geometry_type is null for map details row" },
            point = ViewMapPoint(
                lat = requireNotNull((record.get("lat") as? Number)?.toDouble()) { "lat is null for map details row" },
                lng = requireNotNull((record.get("lng") as? Number)?.toDouble()) { "lng is null for map details row" },
            ),
            regionGeoJson = record.get("region_geojson", String::class.java),
            imagePath = record.get("image_path", String::class.java),
            ratingAvg = (record.get("rating_avg") as? Number)?.toDouble() ?: 0.0,
            ratingsCount = (record.get("ratings_count") as? Number)?.toInt() ?: 0,
            repostsCount = (record.get("reposts_count") as? Number)?.toInt() ?: 0,
            commentsCount = (record.get("comments_count") as? Number)?.toInt() ?: 0,
            categories = categories,
        )
    }

    fun toGeoJson(items: List<ViewMapEventItem>): Map<String, Any> =
        mapOf(
            "type" to "FeatureCollection",
            "features" to items.map { item ->
                mapOf(
                    "type" to "Feature",
                    "geometry" to mapOf(
                        "type" to "Point",
                        "coordinates" to listOf(item.point.lng, item.point.lat),
                    ),
                    "properties" to mapOf(
                        "eventId" to item.eventId,
                        "eventName" to item.eventName,
                        "locationName" to item.locationName,
                        "geometryType" to item.geometryType,
                        "regionGeoJson" to item.regionGeoJson,
                        "distanceMeters" to item.distanceMeters,
                        "ratingAvg" to item.ratingAvg,
                        "ratingsCount" to item.ratingsCount,
                        "categories" to item.categories,
                    ),
                )
            },
        )

    private fun eventItemSql(
        distanceExpression: String,
        conditions: List<String>,
        having: List<String>,
        orderBy: String,
    ): String {
        val havingSql = if (having.isEmpty()) "" else "HAVING ${having.joinToString("\n   AND ")}"
        return """
            SELECT e.id AS event_id,
                   e.name AS event_name,
                   l.name AS location_name,
                   CASE WHEN GeometryType(l.shape)='POINT' THEN 'POINT' WHEN GeometryType(l.shape) IN ('LINESTRING','MULTILINESTRING') THEN 'LINE' ELSE 'REGION' END AS geometry_type,
                   ST_Y(COALESCE(l.point, ST_PointOnSurface(l.shape))) AS lat,
                   ST_X(COALESCE(l.point, ST_PointOnSurface(l.shape))) AS lng,
                   ST_AsGeoJSON(CASE WHEN GeometryType(l.shape)<>'POINT' THEN l.shape END) AS region_geojson,
                   $distanceExpression AS distance_meters,
                   COALESCE(AVG(r.rating), 0)::double precision AS rating_avg,
                   COUNT(r.rating)::int AS ratings_count
            FROM event e
            JOIN location l ON l.id = e.location_id
            LEFT JOIN rating r ON r.event_id = e.id
            WHERE ${conditions.joinToString("\n  AND ")}
            GROUP BY e.id, e.name, e.start_at, l.name, l.point, l.region, l.shape
            $havingSql
            ORDER BY $orderBy
            LIMIT ?
        """.trimIndent()
    }

    private fun addCommonFilters(
        conditions: MutableList<String>,
        bindings: MutableList<Any>,
        categoryIds: List<Long>,
        locationCategoryIds: List<Long>,
        startsFrom: OffsetDateTime?,
        startsTo: OffsetDateTime?,
        onlyActive: Boolean,
        tags: List<String> = emptyList(),
    ) {
        com.evmap.serverapp.features.event.application.addTagFilter(tags,conditions,bindings)
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
        if (locationCategoryIds.isNotEmpty()) {
            val placeholders = locationCategoryIds.joinToString(",") { "?" }
            conditions += "l.category_id IN ($placeholders)"
            bindings.addAll(locationCategoryIds)
        }
        if (startsFrom != null) {
            conditions += "e.start_at >= CAST(? AS timestamptz)"
            bindings += startsFrom
        }
        if (startsTo != null) {
            conditions += "e.start_at <= CAST(? AS timestamptz)"
            bindings += startsTo
        }
        if (onlyActive) {
            conditions += "e.start_at <= now() AND COALESCE(e.end_at, ((e.start_at AT TIME ZONE e.time_zone)::date + 1)::timestamp AT TIME ZONE e.time_zone) > now()"
        }
    }

    private fun boundsCondition(): String =
        """
        (
            ST_Intersects(l.shape, ST_MakeEnvelope(?, ?, ?, ?, 4326))
            OR
            ST_Intersects(l.shape, ST_MakeEnvelope(?, ?, ?, ?, 4326))
        )
        """.trimIndent()

    private fun orderBy(sort: String, includeDistance: Boolean, includeRelevance: Boolean): String {
        val relevance = """
            CASE
                WHEN LOWER(e.name) = LOWER(?) THEN 300
                WHEN LOWER(e.name) LIKE LOWER(?) THEN 200
                WHEN LOWER(l.name) LIKE LOWER(?) THEN 100
                ELSE 0
            END DESC
        """.trimIndent()
        return when (sort) {
            "distance" -> if (includeDistance) "distance_meters ASC NULLS LAST, rating_avg DESC, e.start_at DESC" else "e.start_at DESC"
            "rating" -> "rating_avg DESC, ratings_count DESC, e.start_at DESC"
            "date" -> "e.start_at DESC, rating_avg DESC"
            "relevance" -> if (includeRelevance) "$relevance, rating_avg DESC, e.start_at DESC" else "rating_avg DESC, e.start_at DESC"
            else -> "e.start_at DESC"
        }
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
