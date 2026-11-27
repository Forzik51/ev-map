package com.evmap.serverapp.features.map.api.controller

import com.evmap.serverapp.features.map.api.dto.ViewMapEventItem
import com.evmap.serverapp.features.map.api.dto.ViewMapCluster
import com.evmap.serverapp.features.map.api.dto.ViewMapEventDetails
import com.evmap.serverapp.features.map.application.query.MapQueryService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime

@RestController
@RequestMapping("/api/map/events")
class MapController(
    private val mapQueryService: MapQueryService,
) {
    @GetMapping("/nearby")
    fun nearby(
        @RequestParam lat: Double,
        @RequestParam lng: Double,
        @RequestParam(required = false) radiusMeters: Double?,
        @RequestParam(required = false) categoryIds: List<Long>?,
        @RequestParam(required = false) locationCategoryIds: List<Long>?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startsFrom: OffsetDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startsTo: OffsetDateTime?,
        @RequestParam(required = false) onlyActive: Boolean?,
        @RequestParam(required = false) sort: String?,
        @RequestParam(required = false) limit: Int?,
        @RequestParam(required = false) tags: List<String>? = null,
    ): List<ViewMapEventItem> =
        mapQueryService.findNearby(
            lat = lat,
            lng = lng,
            radiusMeters = radiusMeters,
            categoryIds = categoryIds.orEmpty(),
            locationCategoryIds = locationCategoryIds.orEmpty(),
            startsFrom = startsFrom,
            startsTo = startsTo,
            onlyActive = onlyActive ?: false,
            tags = tags.orEmpty(),
            sort = sort,
            limit = limit,
        )

    @GetMapping("/bounds")
    fun bounds(
        @RequestParam minLat: Double,
        @RequestParam minLng: Double,
        @RequestParam maxLat: Double,
        @RequestParam maxLng: Double,
        @RequestParam(required = false) categoryIds: List<Long>?,
        @RequestParam(required = false) locationCategoryIds: List<Long>?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startsFrom: OffsetDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startsTo: OffsetDateTime?,
        @RequestParam(required = false) onlyActive: Boolean?,
        @RequestParam(required = false) sort: String?,
        @RequestParam(required = false) limit: Int?,
        @RequestParam(required = false) tags: List<String>? = null,
    ): List<ViewMapEventItem> =
        mapQueryService.findBounds(
            minLat = minLat,
            minLng = minLng,
            maxLat = maxLat,
            maxLng = maxLng,
            categoryIds = categoryIds.orEmpty(),
            locationCategoryIds = locationCategoryIds.orEmpty(),
            startsFrom = startsFrom,
            startsTo = startsTo,
            onlyActive = onlyActive ?: false,
            tags = tags.orEmpty(),
            sort = sort,
            limit = limit,
        )

    @GetMapping("/clusters")
    fun clusters(
        @RequestParam minLat: Double,
        @RequestParam minLng: Double,
        @RequestParam maxLat: Double,
        @RequestParam maxLng: Double,
        @RequestParam zoom: Int,
        @RequestParam(required = false) categoryIds: List<Long>?,
        @RequestParam(required = false) locationCategoryIds: List<Long>?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startsFrom: OffsetDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startsTo: OffsetDateTime?,
        @RequestParam(required = false) onlyActive: Boolean?,
        @RequestParam(required = false) tags: List<String>? = null,
    ): List<ViewMapCluster> =
        mapQueryService.findClusters(
            minLat = minLat,
            minLng = minLng,
            maxLat = maxLat,
            maxLng = maxLng,
            zoom = zoom,
            categoryIds = categoryIds.orEmpty(),
            locationCategoryIds = locationCategoryIds.orEmpty(),
            startsFrom = startsFrom,
            startsTo = startsTo,
            onlyActive = onlyActive ?: false,
            tags = tags.orEmpty(),
        )

    @GetMapping("/geojson")
    fun geoJson(
        @RequestParam minLat: Double,
        @RequestParam minLng: Double,
        @RequestParam maxLat: Double,
        @RequestParam maxLng: Double,
        @RequestParam(required = false) categoryIds: List<Long>?,
        @RequestParam(required = false) locationCategoryIds: List<Long>?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startsFrom: OffsetDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startsTo: OffsetDateTime?,
        @RequestParam(required = false) onlyActive: Boolean?,
        @RequestParam(required = false) limit: Int?,
        @RequestParam(required = false) tags: List<String>? = null,
    ): Map<String, Any> =
        mapQueryService.findGeoJson(
            minLat = minLat,
            minLng = minLng,
            maxLat = maxLat,
            maxLng = maxLng,
            categoryIds = categoryIds.orEmpty(),
            locationCategoryIds = locationCategoryIds.orEmpty(),
            startsFrom = startsFrom,
            startsTo = startsTo,
            onlyActive = onlyActive ?: false,
            tags = tags.orEmpty(),
            limit = limit,
        )

    @GetMapping("/{eventId}")
    fun details(@PathVariable eventId: Long): ViewMapEventDetails =
        mapQueryService.findDetails(eventId)

    @GetMapping("/search")
    fun search(
        @RequestParam query: String,
        @RequestParam(required = false) lat: Double?,
        @RequestParam(required = false) lng: Double?,
        @RequestParam(required = false) distanceKm: Double?,
        @RequestParam(required = false) ratingFrom: Double?,
        @RequestParam(required = false) ratingTo: Double?,
        @RequestParam(required = false) categoryIds: List<Long>?,
        @RequestParam(required = false) locationCategoryIds: List<Long>?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startsFrom: OffsetDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startsTo: OffsetDateTime?,
        @RequestParam(required = false) onlyActive: Boolean?,
        @RequestParam(required = false) sort: String?,
        @RequestParam(required = false) limit: Int?,
        @RequestParam(required = false) tags: List<String>? = null,
    ): List<ViewMapEventItem> =
        mapQueryService.search(
            query = query,
            lat = lat,
            lng = lng,
            distanceKm = distanceKm,
            ratingFrom = ratingFrom,
            ratingTo = ratingTo,
            categoryIds = categoryIds.orEmpty(),
            locationCategoryIds = locationCategoryIds.orEmpty(),
            startsFrom = startsFrom,
            startsTo = startsTo,
            onlyActive = onlyActive ?: false,
            tags = tags.orEmpty(),
            sort = sort,
            limit = limit,
        )
}
