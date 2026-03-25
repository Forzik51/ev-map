package com.evmap.serverapp.features.location.application.query

import com.evmap.serverapp.features.location.api.dto.ViewLocationCategory
import com.evmap.serverapp.features.location.infra.read.LocationReadRepository
import org.springframework.stereotype.Service

@Service
class GetAllLocationCategories(
    private val readRepo: LocationReadRepository,
) {
    fun handle(): List<ViewLocationCategory> = readRepo.findAllCategories()
}
