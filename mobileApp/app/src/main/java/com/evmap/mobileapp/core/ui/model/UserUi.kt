package core.ui.model

data class UserUi(
    val id: String,
    val username: String,
    val avatarUrl: String? = null,
    val bio: String? = null
)
