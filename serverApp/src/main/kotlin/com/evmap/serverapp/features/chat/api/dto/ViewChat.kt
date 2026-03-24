package com.evmap.serverapp.features.chat.api.dto

data class ViewChat(
    val id: Long,
    val name: String,
    val usersCount: Int,
    val lastMessageAt: String?,
)
