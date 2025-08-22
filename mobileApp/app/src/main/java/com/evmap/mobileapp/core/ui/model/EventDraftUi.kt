package core.ui.model

data class EventDraftUi(
    val title: String = "bbb",
    val subtitle: String = "bbb",
    val description: String = "bbb",
    val date: String = "bbb",
    val location: String = "bbb",
    val categories: Set<String> = emptySet(),
    val availableCategories: List<String> = emptyList(),
    val media: List<String> = emptyList(),
    val categoriesQuery: String = ""
)
