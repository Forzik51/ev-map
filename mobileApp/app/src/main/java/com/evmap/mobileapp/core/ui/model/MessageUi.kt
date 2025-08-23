package core.ui.model

data class MessageUi(
    val id: String,
    val text: String? = null,
    val mediaUrl: String? = null,
    val timestamp: Long,
    val isMine: Boolean,
    val senderName: String? = null,
    val senderAvatarUrl: String? = null
)

data class ChatUi(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val lastMessage: String? = null,
    val lastMessageTime: String? = null,
    val unreadCount: Int = 0
)
