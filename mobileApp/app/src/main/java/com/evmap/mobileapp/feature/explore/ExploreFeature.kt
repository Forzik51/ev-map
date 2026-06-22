package feature.explore

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.evmap.mobileapp.feature.explore.ui.ExploreViewModel
import core.navigation.FeatureEntry
import core.ui.model.EventUi
import core.ui.model.ProfileUserUi
import feature.explore.ui.ExploreScreen

object ExploreFeature : FeatureEntry {
    override val route = "explore"

    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        onNavigateToRoute: (String) -> Unit,
        onNavigateUp: () -> Unit
    ) {
        navGraphBuilder.composable(route) {
            val viewModel: ExploreViewModel = hiltViewModel()
            val uiState by viewModel.state.collectAsState()

            ExploreScreen(
                uiState = uiState,
                onQueryChange = viewModel::updateQuery,
                onSelectTab = viewModel::selectTab,
                onUserClick = { userId ->
                    onNavigateToRoute("profile/$userId")
                },
                onEventClick = { eventId ->
                    onNavigateToRoute("events/$eventId")
                },
                onOpenFilters = viewModel::openFilters,
                onCloseFilters = viewModel::closeFilters,
                onApplyFilters = viewModel::applyFilters,
                onBack = onNavigateUp,
                onSave = {
                    // Saved-list picker is pending.
                },
                onSortChange = viewModel::updateSortBy,
                onTypeChange = viewModel::updateSelectedType
            )
        }
    }
}

data class ExploreUiState(
    val query: String = "",
    val selectedTab: ExploreTab = ExploreTab.USERS,
    val users: List<ProfileUserUi> = emptyList(),
    val events: List<EventUi> = emptyList(),
    val isFilterOpen: Boolean = true,
    val sortBy: String = "Name",
    val selectedType: String = "All"
)

enum class ExploreTab {
    USERS, EVENTS
}
