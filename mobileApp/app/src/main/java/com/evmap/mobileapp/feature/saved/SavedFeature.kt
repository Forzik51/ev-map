package feature.saved

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import core.navigation.FeatureEntry
import core.ui.model.EventUi
import core.ui.model.GroupItemsUi
import core.ui.model.SavedGroupUi
import core.ui.model.ViewMode
import feature.saved.ui.GroupItemsScreen
import feature.saved.ui.SavedScreen

object SavedFeature : FeatureEntry {
    override val route = "saved"

    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        onNavigateToRoute: (String) -> Unit,
        onNavigateUp: () -> Unit
    ) {
        navGraphBuilder.composable(route = route) {
            // TODO: Replace with actual ViewModel when available
            val savedGroups = remember {
                listOf(
                    SavedGroupUi(
                        id = "group1",
                        name = "Name",
                        count = 5,
                        imageUrl = null
                    ),
                    SavedGroupUi(
                        id = "group2", 
                        name = "Name",
                        count = 3,
                        imageUrl = null
                    ),
                    SavedGroupUi(
                        id = "group3",
                        name = "Name",
                        count = 8,
                        imageUrl = null
                    ),
                    SavedGroupUi(
                        id = "group4",
                        name = "Name", 
                        count = 2,
                        imageUrl = null
                    )
                )
            }

            SavedScreen(
                groups = savedGroups,
                onOpenGroup = { groupId ->
                    onNavigateToRoute("saved/$groupId")
                },
                onAddGroup = {
                    // TODO: Navigate to add/create saved group screen
                    // onNavigateToRoute("saved/create")
                },
                onBack = onNavigateUp
            )
        }

        // Group Items Screen
        navGraphBuilder.composable(route = "saved/{groupId}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable

            // TODO: Replace with actual ViewModel when available
            var groupItemsState by remember {
                mutableStateOf(
                    GroupItemsUi(
                        groupId = groupId,
                        title = "Name", // TODO: Get actual group name
                        items = listOf(
                            EventUi(
                                id = "event1",
                                title = "Title",
                                subtitle = "Subtitle",
                                description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor",
                                imageUrl = null,
                                rating = 5.0f,
                                reviewCount = 50
                            ),
                            EventUi(
                                id = "event2",
                                title = "Title",
                                subtitle = "Subtitle",
                                description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor",
                                imageUrl = null,
                                rating = 4.5f,
                                reviewCount = 30
                            )
                        ),
                        viewMode = ViewMode.LIST
                    )
                )
            }

            GroupItemsScreen(
                ui = groupItemsState,
                onBack = onNavigateUp,
                onToggleView = {
                    groupItemsState = groupItemsState.copy(
                        viewMode = if (groupItemsState.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
                    )
                },
                onOpenItem = { eventId ->
                    // TODO: Navigate to event details
                    // onNavigateToRoute("events/$eventId")
                }
            )
        }
    }
}
