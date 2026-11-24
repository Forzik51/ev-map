package core.ui.model

import com.evmap.mobileapp.core.data.dto.ChatAttachmentDto

data class MessageUi(
    val id: String,
    val text: String? = null,
    val timestamp: Long,
    val isMine: Boolean,
    val senderName: String? = null,
    val senderAvatarUrl: String? = null,
    val attachment: ChatAttachmentDto? = null,
    val isRead: Boolean = false,
)

data class ChatUi(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val lastMessage: String? = null,
    val lastMessageTime: String? = null,
    val unreadCount: Int = 0
)
