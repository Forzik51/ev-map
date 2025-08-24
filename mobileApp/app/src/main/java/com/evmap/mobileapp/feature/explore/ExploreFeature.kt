package feature.explore

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import core.navigation.FeatureEntry
import core.ui.model.EventUi
import core.ui.model.ProfileUserUi
import core.ui.model.UserUi
import feature.explore.ui.ExploreScreen

object ExploreFeature : FeatureEntry {
    override val route = "explore"
    
    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        onNavigateToRoute: (String) -> Unit,
        onNavigateUp: () -> Unit
    ) {
        navGraphBuilder.composable(route) {
            // TODO: Implement with ViewModel
            // val viewModel: ExploreViewModel = hiltViewModel()
            // val state by viewModel.state.collectAsState()
            
            // Mock data for demonstration
            var uiState by remember {
                mutableStateOf(
                    ExploreUiState(
                        query = "",
                        selectedTab = ExploreTab.USERS, // Default to USERS as requested
                        events = listOf(
                            EventUi(
                                id = "1",
                                title = "List item",
                                subtitle = "Category • ",
                                description = "Supporting line text lorem ipsum dolor sit amet, consectetur.",
                                rating = 5.0f,
                                imageUrl = "https://picsum.photos/200/300"
                            ),
                            EventUi(
                                id = "2", 
                                title = "List item",
                                subtitle = "Category • ",
                                description = "Supporting line text lorem ipsum dolor sit amet, consectetur.",
                                rating = 5.0f,
                                imageUrl = "https://picsum.photos/200/300"
                            ),
                            EventUi(
                                id = "3",
                                title = "List item",
                                subtitle = "Category • ", 
                                description = "Supporting line text lorem ipsum dolor sit amet, consectetur.",
                                rating = 5.0f,
                                imageUrl = "https://picsum.photos/200/300"
                            )
                        ),
                        isFilterOpen = false,
                        sortBy = "Name",
                        selectedType = "All"
                    )
                )
            }
            
            ExploreScreen(
                uiState = uiState,
                onQueryChange = { query ->
                    uiState = uiState.copy(query = query)
                    // TODO: viewModel::updateQuery
                },
                onSelectTab = { tab ->
                    uiState = uiState.copy(selectedTab = tab)
                    // TODO: viewModel::selectTab
                },
                onUserClick = { userId ->
                    onNavigateToRoute("profile/$userId")
                    // TODO: viewModel::onUserClick
                },
                onEventClick = { eventId ->
                    onNavigateToRoute("events/$eventId")
                    // TODO: viewModel::onEventClick
                },
                onOpenFilters = {
                    uiState = uiState.copy(isFilterOpen = true)
                    // TODO: viewModel::openFilters
                },
                onCloseFilters = {
                    uiState = uiState.copy(isFilterOpen = false)
                    // TODO: viewModel::closeFilters
                },
                onApplyFilters = {
                    uiState = uiState.copy(isFilterOpen = false)
                    // TODO: viewModel::applyFilters
                },
                onBack = onNavigateUp,
                onSave = { eventId ->
                    // TODO: Handle save/bookmark event
                    // viewModel::saveEvent(eventId)
                },
                onSortChange = { sortBy ->
                    uiState = uiState.copy(sortBy = sortBy)
                    // TODO: viewModel::updateSortBy(sortBy)
                },
                onTypeChange = { type ->
                    uiState = uiState.copy(selectedType = type)
                    // TODO: viewModel::updateSelectedType(type)
                }
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
