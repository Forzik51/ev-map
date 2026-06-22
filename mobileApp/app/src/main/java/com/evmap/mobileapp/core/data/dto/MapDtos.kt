package com.evmap.mobileapp.core.data.dto

data class MapPointDto(
    val lat: Double,
    val lng: Double,
)

data class MapCategoryDto(
    val id: Long,
    val name: String,
)

data class MapEventItemDto(
    val eventId: Long,
    val eventName: String,
    val locationName: String?,
    val geometryType: String,
    val point: MapPointDto,
    val regionGeoJson: String?,
    val distanceMeters: Double?,
    val ratingAvg: Double,
    val ratingsCount: Int,
    val categories: List<MapCategoryDto>,
)

data class MapClusterDto(
    val count: Int,
    val point: MapPointDto,
)

data class MapEventDetailsDto(
    val eventId: Long,
    val eventName: String,
    val description: String,
    val startsAt: String,
    val endsAt: String?,
    val locationName: String?,
    val geometryType: String,
    val point: MapPointDto,
    val regionGeoJson: String?,
    val imagePath: String?,
    val ratingAvg: Double,
    val ratingsCount: Int,
    val repostsCount: Int,
    val commentsCount: Int,
    val categories: List<MapCategoryDto>,
)
