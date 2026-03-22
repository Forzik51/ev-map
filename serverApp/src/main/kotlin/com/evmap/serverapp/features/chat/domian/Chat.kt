package com.evmap.serverapp.features.user.domian

data class Chat (
    val id: Long? = null,
    val username: String,
    val description: String,
    val followers: Int,
    val following: Int,
)