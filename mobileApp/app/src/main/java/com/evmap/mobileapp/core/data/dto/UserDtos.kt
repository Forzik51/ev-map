package com.evmap.mobileapp.core.data.dto

data class UserDto(
    val id: Long,
    val name: String,
    val surname: String?,
    val email: String,
    val phone: String,
    val birthdate: String,
    val username: String,
    val pageDescription: String,
    val path: String?,
    val followersCount: Int,
    val followingCount: Int,
)

data class UserShortDto(
    val id: Long,
    val username: String,
    val pageDescription: String,
)

data class UserPhotoDto(
    val userId: Long,
    val path: String,
)
