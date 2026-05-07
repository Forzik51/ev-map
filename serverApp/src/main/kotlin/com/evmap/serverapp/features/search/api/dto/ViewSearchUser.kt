package com.evmap.serverapp.features.search.api.dto

data class ViewSearchUser(
    val id: Long,
    val username: String,
    val name: String,
    val surname: String?,
    val pageDescription: String,
)
