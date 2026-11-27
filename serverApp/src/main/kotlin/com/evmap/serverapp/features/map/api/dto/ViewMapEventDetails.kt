package com.evmap.serverapp.features.map.api.dto

data class ViewMapEventDetails(
    val eventId: Long,
    val eventName: String,
    val description: String,
    val startsAt: String?,
    val endsAt: String?,
    val locationName: String?,
    val geometryType: String,
    val point: ViewMapPoint,
    val regionGeoJson: String?,
    val imagePath: String?,
    val ratingAvg: Double,
    val ratingsCount: Int,
    val repostsCount: Int,
    val commentsCount: Int,
    val categories: List<ViewMapCategory>,
    val dateMode: String = "DATETIME",
    val startsOn: String? = null,
    val endsOn: String? = null,
    val timeZone: String = "UTC",
)
