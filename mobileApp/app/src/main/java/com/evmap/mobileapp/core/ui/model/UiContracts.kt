package core.ui.model

enum class ViewMode {
    LIST,
    GRID
}

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

data class SavedGroupUi(
    val id: String,
    val name: String,
    val count: Int = 0,
    val imageUrl: String? = null
)

data class GroupItemsUi(
    val groupId: String,
    val title: String,
    val items: List<EventUi>,
    val viewMode: ViewMode = ViewMode.LIST
)
