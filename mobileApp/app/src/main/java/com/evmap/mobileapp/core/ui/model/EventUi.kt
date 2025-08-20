package core.ui.model

data class EventUi(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val imageUrl: String? = null,
    val rating: Float? = null,
    val reviewCount: Int? = null,
    val description: String? = null
)
