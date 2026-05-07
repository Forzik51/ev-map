package com.evmap.serverapp.features.map.api.controller

import com.evmap.serverapp.features.map.api.dto.ViewMapEventItem
import com.evmap.serverapp.features.map.application.query.MapQueryService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

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
        @RequestParam(required = false) limit: Int?,
    ): List<ViewMapEventItem> =
        mapQueryService.findNearby(
            lat = lat,
            lng = lng,
            radiusMeters = radiusMeters,
            categoryIds = categoryIds.orEmpty(),
            limit = limit,
        )

    @GetMapping("/search")
    fun search(
        @RequestParam query: String,
        @RequestParam(required = false) lat: Double?,
        @RequestParam(required = false) lng: Double?,
        @RequestParam(required = false) distanceKm: Double?,
        @RequestParam(required = false) ratingFrom: Double?,
        @RequestParam(required = false) ratingTo: Double?,
        @RequestParam(required = false) categoryIds: List<Long>?,
        @RequestParam(required = false) limit: Int?,
    ): List<ViewMapEventItem> =
        mapQueryService.search(
            query = query,
            lat = lat,
            lng = lng,
            distanceKm = distanceKm,
            ratingFrom = ratingFrom,
            ratingTo = ratingTo,
            categoryIds = categoryIds.orEmpty(),
            limit = limit,
        )
}
