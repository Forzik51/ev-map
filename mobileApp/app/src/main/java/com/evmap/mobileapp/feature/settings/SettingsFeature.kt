package feature.settings

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.evmap.mobileapp.feature.settings.ui.ProfileEditViewModel
import com.evmap.mobileapp.feature.settings.ui.SettingsViewModel
import core.navigation.FeatureEntry
import feature.reports.ReportsFeature
import feature.settings.ui.ProfileEditScreen
import feature.settings.ui.SettingsScreen

object SettingsFeature : FeatureEntry {
    override val route = "settings"

    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        onNavigateToRoute: (String) -> Unit,
        onNavigateUp: () -> Unit
    ) {
        navGraphBuilder.composable(route = route) {
            val viewModel: SettingsViewModel = hiltViewModel()
            val session by viewModel.session.collectAsState()

            SettingsScreen(
                onOpenProfileEdit = {
                    onNavigateToRoute("profile/edit")
                },
                onOpenSaved = {
                    onNavigateToRoute("saved")
                },
                onOpenAdmin = {
                    onNavigateToRoute(ReportsFeature.ModerationRoute)
                },
                showAdmin = session?.isAdmin == true,
                onNavigate = onNavigateToRoute,
                onLogOut = {
                    viewModel.logout()
                },
                onBack = onNavigateUp
            )
        }

        // Profile Edit Screen
        navGraphBuilder.composable(route = "profile/edit") {
            val viewModel: ProfileEditViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()

            LaunchedEffect(state.saved) {
                if (state.saved) {
                    viewModel.consumeSaved()
                    onNavigateUp()
                }
            }

            ProfileEditScreen(
                state = state,
                onEdit = viewModel::edit,
                onChooseAvatar = viewModel::chooseAvatar,
                onRemoveAvatar = viewModel::removeAvatar,
                onUndoAvatarChange = viewModel::undoAvatarChange,
                onApply = viewModel::save,
                onRetry = viewModel::load,
                onBack = onNavigateUp
            )
        }
    }
}
