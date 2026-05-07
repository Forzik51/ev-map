package com.evmap.serverapp.features.search.api.dto

data class ViewSearchResponse(
    val type: String,
    val limit: Int,
    val events: List<ViewSearchEvent> = emptyList(),
    val users: List<ViewSearchUser> = emptyList(),
)
