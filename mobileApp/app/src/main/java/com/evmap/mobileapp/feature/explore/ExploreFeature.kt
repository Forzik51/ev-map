package feature.explore

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.evmap.mobileapp.core.data.dto.EventCategoryDto
import com.evmap.mobileapp.feature.explore.ui.ExploreViewModel
import core.navigation.FeatureEntry
import core.ui.model.EventUi
import core.ui.model.ProfileUserUi
import feature.explore.ui.ExploreScreen
import feature.saved.SavedFeature
import java.util.Base64

object ExploreFeature : FeatureEntry {
    override val route = "explore"
    internal const val QUERY_ARGUMENT = "query"

    fun routeForQuery(query: String): String {
        if (query.isBlank()) return route
        val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(query.encodeToByteArray())
        return "$route?$QUERY_ARGUMENT=$encoded"
    }

    internal fun decodeQuery(argument: String?): String =
        argument?.let { runCatching { Base64.getUrlDecoder().decode(it).decodeToString() }.getOrNull() }.orEmpty()

    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        onNavigateToRoute: (String) -> Unit,
        onNavigateUp: () -> Unit
    ) {
        navGraphBuilder.composable(
            route = "$route?$QUERY_ARGUMENT={$QUERY_ARGUMENT}",
            arguments = listOf(navArgument(QUERY_ARGUMENT) {
                type = NavType.StringType
                defaultValue = ""
            }),
        ) {
            val viewModel: ExploreViewModel = hiltViewModel()
            val uiState by viewModel.state.collectAsStateWithLifecycle()

            ExploreScreen(
                uiState = uiState,
                onQueryChange = viewModel::updateQuery,
                onSearch = viewModel::submitSearch,
                onSelectTab = viewModel::selectTab,
                onUserClick = { userId ->
                    onNavigateToRoute("profile/$userId")
                },
                onEventClick = { eventId ->
                    onNavigateToRoute("events/$eventId")
                },
                onSaveEvent = { onNavigateToRoute(SavedFeature.pickerRoute(it)) },
                onOpenFilters = viewModel::openFilters,
                onCloseFilters = viewModel::closeFilters,
                onApplyFilters = viewModel::applyFilters,
                onBack = onNavigateUp,
                onSortChange = viewModel::updateSort,
                onCategoryChange = viewModel::updateCategory,
                onTagsChange = viewModel::updateTags,
                onRetryCategories = viewModel::loadCategories,
            )
        }
    }
}

data class ExploreUiState(
    val query: String = "",
    val selectedTab: ExploreTab = ExploreTab.EVENTS,
    val users: List<ProfileUserUi> = emptyList(),
    val events: List<EventUi> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val filters: ExploreFilters = ExploreFilters(),
    val draftFilters: ExploreFilters? = null,
    val categories: List<EventCategoryDto>? = null,
    val loadingCategories: Boolean = false,
    val categoryLoadFailed: Boolean = false,
)

data class ExploreFilters(
    val sort: ExploreSort = ExploreSort.RELEVANCE,
    val categoryId: Long? = null,
    val tagsText: String = "",
)

enum class ExploreTab(val apiValue: String) {
    USERS("users"), EVENTS("events")
}

enum class ExploreSort(val apiValue: String, val label: String) {
    RELEVANCE("relevance", "Relevance"),
    DATE("date", "Date"),
    RATING("rating", "Rating"),
}
