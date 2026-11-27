package com.evmap.serverapp.features.search.api.controller

import com.evmap.serverapp.features.search.api.dto.ViewSearchResponse
import com.evmap.serverapp.features.search.api.dto.ViewSearchSuggestion
import com.evmap.serverapp.features.search.application.query.SearchService
import com.evmap.serverapp.shared.security.CurrentUserProvider
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime

@RestController
@RequestMapping("/api/search")
class SearchController(
    private val searchService: SearchService,
    private val currentUserProvider: CurrentUserProvider,
) {
    @GetMapping
    fun search(
        @RequestParam query: String,
        @RequestParam(required = false) type: String?,
        @RequestParam(required = false) lat: Double?,
        @RequestParam(required = false) lng: Double?,
        @RequestParam(required = false) distanceKm: Double?,
        @RequestParam(required = false) ratingFrom: Double?,
        @RequestParam(required = false) ratingTo: Double?,
        @RequestParam(required = false) categoryIds: List<Long>?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startsFrom: OffsetDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startsTo: OffsetDateTime?,
        @RequestParam(required = false) onlyUpcoming: Boolean?,
        @RequestParam(required = false) sort: String?,
        @RequestParam(required = false) limit: Int?,
        @RequestParam(required = false) tags: List<String>? = null,
    ): ViewSearchResponse =
        searchService.handle(
            userId = currentUserProvider.get().id,
            query = query,
            type = type,
            lat = lat,
            lng = lng,
            distanceKm = distanceKm,
            ratingFrom = ratingFrom,
            ratingTo = ratingTo,
            categoryIds = categoryIds.orEmpty(),
            startsFrom = startsFrom,
            startsTo = startsTo,
            onlyUpcoming = onlyUpcoming ?: false,
            sort = sort,
            limit = limit,
            tags = tags.orEmpty(),
        )

    @GetMapping("/suggestions")
    fun suggestions(
        @RequestParam query: String,
        @RequestParam(required = false) limit: Int?,
    ): List<ViewSearchSuggestion> =
        searchService.suggestions(query, limit)

    @GetMapping("/popular")
    fun popular(
        @RequestParam(required = false) limit: Int?,
    ): List<ViewSearchSuggestion> =
        searchService.popular(limit)
}
