package com.evmap.serverapp.features.user.domian

import java.time.LocalDate

data class User(
    val id: Long? = null,
    val name: String,
    val surname: String?,
    val email: String,
    val phone: String,
    val birthdate: LocalDate,
    val passwordHash: String,
    val username: String,
    val pageDescription: String,
)
