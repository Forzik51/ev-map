package com.evmap.serverapp.features.list.api.dto

data class ViewList(
    val id: Long,
    val name: String,
    val userId: Long,
    val eventsCount: Int,
)
