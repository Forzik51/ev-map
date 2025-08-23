package core.ui.model

data class ProfileUiState(
    val user: ProfileUserUi? = null,
    val list: List<EventUi> = emptyList(),
    val isMe: Boolean = false,
    val isFollowing: Boolean = false,
    val selectedViewIsGrid: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class ProfileUserUi(
    val id: String,
    val username: String,
    val avatarUrl: String? = null,
    val title: String? = null,
    val bio: String? = null,
    val followers: Int = 0,
    val following: Int = 0
)

data class UserFollowUi(
    val user: ProfileUserUi,
    val isFollowing: Boolean = false
)
