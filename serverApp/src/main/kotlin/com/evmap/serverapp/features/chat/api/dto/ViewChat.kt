package com.evmap.serverapp.features.chat.api.dto

data class ViewChat(
    val id: Long,
    val name: String,
    val usersCount: Int,
    val lastMessageAt: String?,
    val userIds: List<Long>,
    val lastMessageText: String? = null,
    val lastAttachmentKind: String? = null,
    val unreadCount: Int = 0,
)
