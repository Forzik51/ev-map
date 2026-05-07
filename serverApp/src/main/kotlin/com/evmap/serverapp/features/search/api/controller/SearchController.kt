package com.evmap.serverapp.features.search.api.controller

import com.evmap.serverapp.features.search.api.dto.ViewSearchResponse
import com.evmap.serverapp.features.search.application.query.SearchService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/search")
class SearchController(
    private val searchService: SearchService,
) {
    @GetMapping
    fun search(
        @RequestParam query: String,
        @RequestParam(required = false) lat: Double?,
        @RequestParam(required = false) lng: Double?,
        @RequestParam(required = false) distanceKm: Double?,
        @RequestParam(required = false) ratingFrom: Double?,
        @RequestParam(required = false) ratingTo: Double?,
        @RequestParam(required = false) categoryIds: List<Long>?,
        @RequestParam(required = false) limit: Int?,
    ): ViewSearchResponse =
        searchService.handle(
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
