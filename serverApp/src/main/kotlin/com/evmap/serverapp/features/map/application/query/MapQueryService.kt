package com.evmap.serverapp.features.map.application.query

import com.evmap.serverapp.features.map.api.dto.ViewMapEventItem
import com.evmap.serverapp.features.map.infra.read.MapReadRepository
import com.evmap.serverapp.features.search.application.query.SearchService
import org.springframework.stereotype.Service

@Service
class MapQueryService(
    private val readRepo: MapReadRepository,
    private val searchService: SearchService,
) {
    fun findNearby(
        lat: Double,
        lng: Double,
        radiusMeters: Double?,
        categoryIds: List<Long>,
        limit: Int?,
    ): List<ViewMapEventItem> {
        val effectiveLimit = searchService.normalizeLimit(limit)
        val effectiveRadius = radiusMeters ?: 100.0
        if (effectiveRadius <= 0.0) throw IllegalArgumentException("radiusMeters must be positive")
        searchService.validateCategoryIds(categoryIds)

        return readRepo.findNearby(
            lat = lat,
            lng = lng,
            radiusMeters = effectiveRadius,
            categoryIds = categoryIds.distinct(),
            limit = effectiveLimit,
        )
    }

    fun search(
        query: String,
        lat: Double?,
        lng: Double?,
        distanceKm: Double?,
        ratingFrom: Double?,
        ratingTo: Double?,
        categoryIds: List<Long>,
        limit: Int?,
    ): List<ViewMapEventItem> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) throw IllegalArgumentException("query must not be blank")

        val effectiveLimit = searchService.normalizeLimit(limit)
        searchService.validateCoordinates(lat, lng)
        searchService.validateDistance(distanceKm)
        searchService.validateRatingRange(ratingFrom, ratingTo)
        searchService.validateCategoryIds(categoryIds)
        if (distanceKm != null && (lat == null || lng == null)) {
            throw IllegalArgumentException("lat and lng are required when distanceKm is provided")
        }

        return readRepo.search(
            query = normalizedQuery,
            lat = lat,
            lng = lng,
            distanceKm = distanceKm,
            ratingFrom = ratingFrom,
            ratingTo = ratingTo,
            categoryIds = categoryIds.distinct(),
            limit = effectiveLimit,
        )
    }
}
