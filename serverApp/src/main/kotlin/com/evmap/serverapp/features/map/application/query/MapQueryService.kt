package com.evmap.serverapp.features.map.application.query

import com.evmap.serverapp.features.map.api.dto.ViewMapCluster
import com.evmap.serverapp.features.map.api.dto.ViewMapEventDetails
import com.evmap.serverapp.features.map.api.dto.ViewMapEventItem
import com.evmap.serverapp.features.map.infra.read.MapReadRepository
import com.evmap.serverapp.features.search.application.query.SearchService
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

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
        locationCategoryIds: List<Long>,
        startsFrom: OffsetDateTime?,
        startsTo: OffsetDateTime?,
        onlyActive: Boolean,
        sort: String?,
        limit: Int?,
    ): List<ViewMapEventItem> {
        val effectiveLimit = searchService.normalizeLimit(limit)
        val effectiveRadius = radiusMeters ?: 100.0
        if (effectiveRadius <= 0.0) throw IllegalArgumentException("radiusMeters must be positive")
        searchService.validateCategoryIds(categoryIds)
        searchService.validateCategoryIds(locationCategoryIds)
        validateSort(sort)

        return readRepo.findNearby(
            lat = lat,
            lng = lng,
            radiusMeters = effectiveRadius,
            categoryIds = categoryIds.distinct(),
            locationCategoryIds = locationCategoryIds.distinct(),
            startsFrom = startsFrom,
            startsTo = startsTo,
            onlyActive = onlyActive,
            sort = sort ?: "distance",
            limit = effectiveLimit,
        )
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
        sort: String?,
        limit: Int?,
    ): List<ViewMapEventItem> {
        validateBounds(minLat, minLng, maxLat, maxLng)
        searchService.validateCategoryIds(categoryIds)
        searchService.validateCategoryIds(locationCategoryIds)
        validateSort(sort)

        return readRepo.findBounds(
            minLat = minLat,
            minLng = minLng,
            maxLat = maxLat,
            maxLng = maxLng,
            categoryIds = categoryIds.distinct(),
            locationCategoryIds = locationCategoryIds.distinct(),
            startsFrom = startsFrom,
            startsTo = startsTo,
            onlyActive = onlyActive,
            sort = sort ?: "date",
            limit = searchService.normalizeLimit(limit),
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
    ): List<ViewMapCluster> {
        validateBounds(minLat, minLng, maxLat, maxLng)
        if (zoom < 0 || zoom > 22) throw IllegalArgumentException("zoom must be in range 0..22")
        searchService.validateCategoryIds(categoryIds)
        searchService.validateCategoryIds(locationCategoryIds)
        return readRepo.findClusters(
            minLat = minLat,
            minLng = minLng,
            maxLat = maxLat,
            maxLng = maxLng,
            zoom = zoom,
            categoryIds = categoryIds.distinct(),
            locationCategoryIds = locationCategoryIds.distinct(),
            startsFrom = startsFrom,
            startsTo = startsTo,
            onlyActive = onlyActive,
        )
    }

    fun findGeoJson(
        minLat: Double,
        minLng: Double,
        maxLat: Double,
        maxLng: Double,
        categoryIds: List<Long>,
        locationCategoryIds: List<Long>,
        startsFrom: OffsetDateTime?,
        startsTo: OffsetDateTime?,
        onlyActive: Boolean,
        limit: Int?,
    ): Map<String, Any> =
        readRepo.toGeoJson(
            findBounds(
                minLat = minLat,
                minLng = minLng,
                maxLat = maxLat,
                maxLng = maxLng,
                categoryIds = categoryIds,
                locationCategoryIds = locationCategoryIds,
                startsFrom = startsFrom,
                startsTo = startsTo,
                onlyActive = onlyActive,
                sort = "date",
                limit = limit,
            ),
        )

    fun findDetails(eventId: Long): ViewMapEventDetails = readRepo.findDetails(eventId)

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
        sort: String?,
        limit: Int?,
    ): List<ViewMapEventItem> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) throw IllegalArgumentException("query must not be blank")

        val effectiveLimit = searchService.normalizeLimit(limit)
        searchService.validateCoordinates(lat, lng)
        searchService.validateDistance(distanceKm)
        searchService.validateRatingRange(ratingFrom, ratingTo)
        searchService.validateCategoryIds(categoryIds)
        searchService.validateCategoryIds(locationCategoryIds)
        validateSort(sort)
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
            locationCategoryIds = locationCategoryIds.distinct(),
            startsFrom = startsFrom,
            startsTo = startsTo,
            onlyActive = onlyActive,
            sort = sort ?: "relevance",
            limit = effectiveLimit,
        )
    }

    private fun validateBounds(minLat: Double, minLng: Double, maxLat: Double, maxLng: Double) {
        if (minLat !in -90.0..90.0 || maxLat !in -90.0..90.0) throw IllegalArgumentException("lat bounds must be in range -90..90")
        if (minLng !in -180.0..180.0 || maxLng !in -180.0..180.0) throw IllegalArgumentException("lng bounds must be in range -180..180")
        if (minLat > maxLat) throw IllegalArgumentException("minLat must be less than or equal to maxLat")
        if (minLng > maxLng) throw IllegalArgumentException("minLng must be less than or equal to maxLng")
    }

    private fun validateSort(sort: String?) {
        if (sort == null) return
        if (sort !in setOf("distance", "rating", "date", "relevance")) {
            throw IllegalArgumentException("sort must be one of distance, rating, date, relevance")
        }
    }
}
