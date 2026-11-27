package com.evmap.mobileapp.core.data.dto

data class UserDto(
    val id: Long,
    val name: String,
    val surname: String?,
    val email: String,
    val phone: String?,
    val birthdate: String?,
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
    val path: String? = null,
)

data class UpdateProfileRequest(
    val name: String = "",
    val surname: String? = null,
    val username: String = "",
    val phone: String = "",
    val pageDescription: String = "",
    val dateOfBirth: String? = null,
    val updateDateOfBirth: Boolean = true,
)

data class UserPhotoDto(
    val userId: Long,
    val path: String,
)
