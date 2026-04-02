package com.evmap.serverapp.shared.email.domain

data class EmailMessage(
    val to: String,
    val subject: String,
    val body: String,
)
