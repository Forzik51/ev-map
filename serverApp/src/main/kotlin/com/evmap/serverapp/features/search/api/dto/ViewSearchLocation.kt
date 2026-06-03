package com.evmap.serverapp.features.search.api.dto

data class ViewSearchLocation(
    val id: Long,
    val name: String,
    val categoryId: Long,
    val categoryName: String?,
)
