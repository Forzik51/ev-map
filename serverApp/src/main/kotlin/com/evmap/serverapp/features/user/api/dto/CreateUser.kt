package com.evmap.serverapp.features.user.api.dto

import java.time.Instant

data class CreateUser (
    val name: String,
    val surname: String,
    val email: String,
    val phone: String,
    val dateOfBirth: Instant,
    val username: String,
    val description: String,
    val passwd: String,
)