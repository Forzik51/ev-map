package com.evmap.serverapp.features.map.api.dto

data class ViewMapEventItem(
    val eventId: Long,
    val eventName: String,
    val locationName: String?,
    val geometryType: String,
    val point: ViewMapPoint,
    val regionGeoJson: String?,
    val distanceMeters: Double?,
    val ratingAvg: Double,
    val ratingsCount: Int,
    val categories: List<ViewMapCategory>,
)
