package com.evmap.mobileapp.core.data.dto

data class SearchCategoryDto(
    val id: Long,
    val name: String,
)

data class SearchEventDto(
    val id: Long,
    val name: String,
    val description: String,
    val locationName: String?,
    val startsAt: String,
    val ratingAvg: Double,
    val ratingsCount: Int,
    val distanceMeters: Double?,
    val categories: List<SearchCategoryDto>,
    val matchedFields: List<String> = emptyList(),
)

data class SearchUserDto(
    val id: Long,
    val username: String,
    val name: String,
    val surname: String?,
    val pageDescription: String,
)

data class SearchLocationDto(
    val id: Long,
    val name: String,
    val categoryId: Long,
    val categoryName: String?,
)

data class SearchResponseDto(
    val type: String,
    val limit: Int,
    val events: List<SearchEventDto> = emptyList(),
    val users: List<SearchUserDto> = emptyList(),
    val locations: List<SearchLocationDto> = emptyList(),
    val categories: List<SearchCategoryDto> = emptyList(),
)

data class SearchSuggestionDto(
    val type: String,
    val value: String,
    val label: String,
)
