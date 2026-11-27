package com.evmap.serverapp.features.location.api.controller

import com.evmap.serverapp.features.location.api.dto.ViewLocationCategory
import com.evmap.serverapp.features.location.application.query.GetAllLocationCategories
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.*
import com.evmap.serverapp.features.location.application.*
import com.evmap.serverapp.shared.security.CurrentUserProvider

@RestController
@RequestMapping("/api/locations")
class LocationController(
    private val getAllLocationCategories: GetAllLocationCategories,
    private val locations: LocationService,
    private val currentUser: CurrentUserProvider,
) {
    @GetMapping("/categories")
    fun getAllCategories(): List<ViewLocationCategory> = getAllLocationCategories.handle()

    @GetMapping("/search") fun search(@RequestParam query: String) = locations.search(query,currentUser.get().id)
    @GetMapping("/{id}") fun get(@PathVariable id: Long) = locations.get(id)
    @PostMapping fun create(@RequestBody request: CreateLocationRequest) = locations.create(request,currentUser.get().id)
    @PostMapping("/resolve") fun resolve(@RequestBody request: ResolveLocationRequest) = locations.resolve(request.candidateId,currentUser.get().id)
}
