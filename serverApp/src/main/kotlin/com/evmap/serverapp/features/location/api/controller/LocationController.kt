package com.evmap.serverapp.features.location.api.controller

import com.evmap.serverapp.features.location.api.dto.ViewLocationCategory
import com.evmap.serverapp.features.location.application.query.GetAllLocationCategories
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/locations")
class LocationController(
    private val getAllLocationCategories: GetAllLocationCategories,
) {
    @GetMapping("/categories")
    fun getAllCategories(): List<ViewLocationCategory> = getAllLocationCategories.handle()
}
