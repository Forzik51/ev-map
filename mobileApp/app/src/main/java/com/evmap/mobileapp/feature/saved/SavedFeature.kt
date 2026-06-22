package feature.saved

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.evmap.mobileapp.feature.saved.ui.SavedViewModel
import core.navigation.FeatureEntry
import core.ui.model.GroupItemsUi
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
            val viewModel: SavedViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()

            LaunchedEffect(Unit) {
                viewModel.loadGroups()
            }

            SavedScreen(
                groups = state.groups,
                onOpenGroup = { groupId ->
                    onNavigateToRoute("saved/$groupId")
                },
                onAddGroup = {
                    // Create-list UI is still pending.
                },
                onBack = onNavigateUp
            )
        }

        navGraphBuilder.composable(route = "saved/{groupId}") { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            val viewModel: SavedViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()

            LaunchedEffect(groupId) {
                viewModel.loadGroup(groupId)
            }

            GroupItemsScreen(
                ui = state.groupItems ?: GroupItemsUi(groupId = groupId, title = "Saved", items = emptyList()),
                onBack = onNavigateUp,
                onToggleView = viewModel::toggleView,
                onOpenItem = { eventId ->
                    onNavigateToRoute("events/$eventId")
                }
            )
        }
    }
}
