package com.evmap.mobileapp.core.data.dto

data class CreateListRequest(
    val name: String,
)

data class ListDto(
    val id: Long,
    val name: String,
    val userId: Long,
    val eventsCount: Int,
)
