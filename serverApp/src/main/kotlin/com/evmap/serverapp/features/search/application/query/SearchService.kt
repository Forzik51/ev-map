package com.evmap.serverapp.features.search.application.query

import com.evmap.serverapp.features.search.api.dto.ViewSearchResponse
import com.evmap.serverapp.features.search.infra.read.SearchReadRepository
import org.springframework.stereotype.Service

@Service
class SearchService(
    private val readRepo: SearchReadRepository,
) {
    fun handle(
        query: String,
        lat: Double?,
        lng: Double?,
        distanceKm: Double?,
        ratingFrom: Double?,
        ratingTo: Double?,
        categoryIds: List<Long>,
        limit: Int?,
    ): ViewSearchResponse {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) throw IllegalArgumentException("query must not be blank")

        val effectiveLimit = normalizeLimit(limit)
        validateCoordinates(lat, lng)
        validateDistance(distanceKm)
        validateRatingRange(ratingFrom, ratingTo)
        validateCategoryIds(categoryIds)

        return if (normalizedQuery.startsWith("@")) {
            ViewSearchResponse(
                type = "users",
                limit = effectiveLimit,
                users = readRepo.searchUsers(normalizedQuery, effectiveLimit),
            )
        } else {
            if (distanceKm != null && (lat == null || lng == null)) {
                throw IllegalArgumentException("lat and lng are required when distanceKm is provided")
            }

            ViewSearchResponse(
                type = "events",
                limit = effectiveLimit,
                events = readRepo.searchEvents(
                    query = normalizedQuery,
                    lat = lat,
                    lng = lng,
                    distanceKm = distanceKm,
                    ratingFrom = ratingFrom,
                    ratingTo = ratingTo,
                    categoryIds = categoryIds.distinct(),
                    limit = effectiveLimit,
                ),
            )
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
}
