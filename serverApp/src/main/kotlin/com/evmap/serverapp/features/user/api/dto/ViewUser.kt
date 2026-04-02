package com.evmap.serverapp.features.user.api.dto

import java.time.LocalDate

data class ViewUser(
    val id: Long,
    val name: String,
    val surname: String?,
    val email: String,
    val phone: String,
    val birthdate: LocalDate,
    val username: String,
    val pageDescription: String,
    val path: String?,
    val followersCount: Int,
    val followingCount: Int,
)
