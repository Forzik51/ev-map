package core.ui.model

@kotlinx.serialization.Serializable
data class EventUi(
    val id: String,
    val title: String,
    val location: String,
    val startsAt: String,
    val imageUrl: String? = null,
    val rating: Float? = null,
    val reviewCount: Int? = null,
    val description: String
)
