package core.ui.model

data class FeedItemUi(
    val id: String,
    val event: EventUi,
    val timestamp: Long? = null,
    val isPromoted: Boolean = false
)

data class EventDetailsUi(
    val event: EventUi,
    val gallery: List<String> = emptyList(),
    val fullDescription: String? = null,
    val location: String? = null,
    val date: String? = null,
    val categories: List<String> = emptyList(),
    val viewCount: Int? = null,
    val likeCount: Int? = null,
    val organizerName: String? = null,
    val organizerId: String? = null
)

