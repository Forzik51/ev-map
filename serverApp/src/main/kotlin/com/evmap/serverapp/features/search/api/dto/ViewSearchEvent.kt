package com.evmap.serverapp.features.search.api.dto

data class ViewSearchEvent(
    val id: Long,
    val name: String,
    val description: String,
    val locationName: String?,
    val startsAt: String,
    val ratingAvg: Double,
    val ratingsCount: Int,
    val distanceMeters: Double?,
    val categories: List<ViewSearchCategory>,
)
