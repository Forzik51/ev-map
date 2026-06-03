package com.evmap.serverapp.features.search.application.query

import com.evmap.serverapp.features.search.api.dto.ViewSearchResponse
import com.evmap.serverapp.features.search.api.dto.ViewSearchSuggestion
import com.evmap.serverapp.features.search.infra.read.SearchReadRepository
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class SearchService(
    private val readRepo: SearchReadRepository,
) {
    fun handle(
        userId: Long,
        query: String,
        type: String?,
        lat: Double?,
        lng: Double?,
        distanceKm: Double?,
        ratingFrom: Double?,
        ratingTo: Double?,
        categoryIds: List<Long>,
        startsFrom: OffsetDateTime?,
        startsTo: OffsetDateTime?,
        onlyUpcoming: Boolean,
        sort: String?,
        limit: Int?,
    ): ViewSearchResponse {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) throw IllegalArgumentException("query must not be blank")

        val effectiveLimit = normalizeLimit(limit)
        validateCoordinates(lat, lng)
        validateDistance(distanceKm)
        validateRatingRange(ratingFrom, ratingTo)
        validateCategoryIds(categoryIds)
        validateSearchType(type)
        validateSort(sort)
        readRepo.recordSearch(userId, normalizedQuery, type ?: if (normalizedQuery.startsWith("@")) "users" else "events")

        val resolvedType = type ?: if (normalizedQuery.startsWith("@")) "users" else "events"
        return when (resolvedType) {
            "users" -> ViewSearchResponse(
                type = "users",
                limit = effectiveLimit,
                users = readRepo.searchUsers(normalizedQuery, effectiveLimit),
            )
            "locations" -> ViewSearchResponse(
                type = "locations",
                limit = effectiveLimit,
                locations = readRepo.searchLocations(normalizedQuery, effectiveLimit),
            )
            "categories" -> ViewSearchResponse(
                type = "categories",
                limit = effectiveLimit,
                categories = readRepo.searchCategories(normalizedQuery, effectiveLimit),
            )
            "all" -> ViewSearchResponse(
                type = "all",
                limit = effectiveLimit,
                users = readRepo.searchUsers(normalizedQuery, effectiveLimit.coerceAtMost(10)),
                events = searchEvents(normalizedQuery, lat, lng, distanceKm, ratingFrom, ratingTo, categoryIds, startsFrom, startsTo, onlyUpcoming, sort, effectiveLimit.coerceAtMost(10)),
                locations = readRepo.searchLocations(normalizedQuery, effectiveLimit.coerceAtMost(10)),
                categories = readRepo.searchCategories(normalizedQuery, effectiveLimit.coerceAtMost(10)),
            )
            else -> {
            if (distanceKm != null && (lat == null || lng == null)) {
                throw IllegalArgumentException("lat and lng are required when distanceKm is provided")
            }

            ViewSearchResponse(
                type = "events",
                limit = effectiveLimit,
                events = searchEvents(normalizedQuery, lat, lng, distanceKm, ratingFrom, ratingTo, categoryIds, startsFrom, startsTo, onlyUpcoming, sort, effectiveLimit),
            )
            }
        }
    }

    fun normalizeLimit(limit: Int?): Int {
        val resolved = limit ?: 100
        if (resolved <= 0) throw IllegalArgumentException("limit must be positive")
        return resolved.coerceAtMost(100)
    }

    fun validateDistance(distanceKm: Double?) {
        if (distanceKm == null) return
        if (distanceKm < 0.0 || distanceKm > 5.0) {
            throw IllegalArgumentException("distanceKm must be in range 0..5")
        }
    }

    fun validateRatingRange(ratingFrom: Double?, ratingTo: Double?) {
        if (ratingFrom != null && (ratingFrom < 0.0 || ratingFrom > 5.0)) {
            throw IllegalArgumentException("ratingFrom must be in range 0..5")
        }
        if (ratingTo != null && (ratingTo < 0.0 || ratingTo > 5.0)) {
            throw IllegalArgumentException("ratingTo must be in range 0..5")
        }
        if (ratingFrom != null && ratingTo != null && ratingFrom > ratingTo) {
            throw IllegalArgumentException("ratingFrom must be less than or equal to ratingTo")
        }
    }

    fun validateCoordinates(lat: Double?, lng: Double?) {
        if ((lat == null) != (lng == null)) {
            throw IllegalArgumentException("lat and lng must be provided together")
        }
    }

    fun validateCategoryIds(categoryIds: List<Long>) {
        if (categoryIds.any { it <= 0 }) {
            throw IllegalArgumentException("categoryIds must contain only positive ids")
        }
    }

    fun suggestions(query: String, limit: Int?): List<ViewSearchSuggestion> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) return emptyList()
        return readRepo.suggestions(normalizedQuery, normalizeLimit(limit).coerceAtMost(20))
    }

    fun popular(limit: Int?): List<ViewSearchSuggestion> =
        readRepo.popularSearches(normalizeLimit(limit).coerceAtMost(20))

    private fun searchEvents(
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
        sort: String?,
        limit: Int,
    ) = readRepo.searchEvents(
        query = query,
        lat = lat,
        lng = lng,
        distanceKm = distanceKm,
        ratingFrom = ratingFrom,
        ratingTo = ratingTo,
        categoryIds = categoryIds.distinct(),
        startsFrom = startsFrom,
        startsTo = startsTo,
        onlyUpcoming = onlyUpcoming,
        sort = sort ?: "relevance",
        limit = limit,
    )

    private fun validateSearchType(type: String?) {
        if (type == null) return
        if (type !in setOf("events", "users", "all", "locations", "categories")) {
            throw IllegalArgumentException("type must be one of events, users, all, locations, categories")
        }
    }

    private fun validateSort(sort: String?) {
        if (sort == null) return
        if (sort !in setOf("relevance", "rating", "distance", "date")) {
            throw IllegalArgumentException("sort must be one of relevance, rating, distance, date")
        }
    }
}
