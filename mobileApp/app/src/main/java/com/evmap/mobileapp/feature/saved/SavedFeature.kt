package feature.saved

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import com.evmap.mobileapp.feature.saved.ui.SavedViewModel
import core.navigation.FeatureEntry
import feature.saved.ui.GroupItemsScreen
import feature.saved.ui.SavedListDialogs
import feature.saved.ui.SavedListPicker
import feature.saved.ui.SavedScreen

object SavedFeature : FeatureEntry {
    override val route = "saved"

    fun pickerRoute(eventId: String) = "$route/pick/$eventId"

    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        onNavigateToRoute: (String) -> Unit,
        onNavigateUp: () -> Unit,
    ) {
        navGraphBuilder.composable(route) {
            val viewModel: SavedViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }

            SavedScreen(
                state = state,
                onOpenGroup = { onNavigateToRoute("$route/$it") },
                onAddGroup = viewModel::openNameDialog,
                onRetry = viewModel::load,
                onBack = onNavigateUp,
            )
            SavedListDialogs(state, viewModel)
        }

        navGraphBuilder.composable("$route/{groupId}", arguments = listOf(navArgument("groupId") { type = NavType.LongType })) {
            val viewModel: SavedViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }
            LaunchedEffect(state.deleted) { if (state.deleted) onNavigateUp() }

            GroupItemsScreen(
                state = state,
                onBack = onNavigateUp,
                onRetry = viewModel::load,
                onToggleView = viewModel::toggleView,
                onRename = viewModel::openNameDialog,
                onDelete = viewModel::confirmDelete,
                onRemoveEvent = viewModel::removeEvent,
                onOpenItem = { onNavigateToRoute("events/$it") },
                onSaveItem = { onNavigateToRoute(pickerRoute(it)) },
            )
            SavedListDialogs(state, viewModel)
        }

        navGraphBuilder.dialog("$route/pick/{eventId}", arguments = listOf(navArgument("eventId") { type = NavType.LongType })) {
            val viewModel: SavedViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }
            SavedListPicker(state, viewModel, onClose = onNavigateUp)
        }
    }
}
