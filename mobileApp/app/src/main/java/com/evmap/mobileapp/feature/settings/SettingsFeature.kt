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
            val state by viewModel.state.collectAsState()

            LaunchedEffect(state.loggedOut) {
                if (state.loggedOut) {
                    onNavigateToRoute("auth/login")
                }
            }

            SettingsScreen(
                onOpenProfileEdit = {
                    onNavigateToRoute("profile/edit")
                },
                onOpenSaved = {
                    onNavigateToRoute("saved")
                },
                onOpenAdmin = {
                    onNavigateToRoute("admin/moderation")
                },
                showAdmin = state.isAdmin,
                onOpenOthers = {
                    // TODO: Navigate to others screen
                    // onNavigateToRoute("others")
                },
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
                username = state.username,
                phone = state.phone,
                title = state.title,
                bio = state.bio,
                onUsernameChange = { },
                onPhoneChange = { },
                onTitleChange = viewModel::updateTitle,
                onBioChange = viewModel::updateBio,
                onApply = viewModel::save,
                onBack = onNavigateUp
            )
        }
    }
}
