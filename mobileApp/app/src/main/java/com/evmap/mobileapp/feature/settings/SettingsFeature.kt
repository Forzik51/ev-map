package feature.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import core.navigation.FeatureEntry
import feature.settings.ui.SettingsScreen
import feature.settings.ui.ProfileEditScreen

object SettingsFeature : FeatureEntry {
    override val route = "settings"

    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        onNavigateToRoute: (String) -> Unit,
        onNavigateUp: () -> Unit
    ) {
        navGraphBuilder.composable(route = route) {
            SettingsScreen(
                onOpenProfileEdit = {
                    onNavigateToRoute("profile/edit")
                },
                onOpenSaved = {
                    onNavigateToRoute("saved")
                },
                onOpenOthers = {
                    // TODO: Navigate to others screen
                    // onNavigateToRoute("others")
                },
                onBack = onNavigateUp
            )
        }

        // Profile Edit Screen
        navGraphBuilder.composable(route = "profile/edit") {
            var username by remember { mutableStateOf("") }
            var phone by remember { mutableStateOf("") }
            var title by remember { mutableStateOf("") }
            var bio by remember { mutableStateOf("") }

            ProfileEditScreen(
                username = username,
                phone = phone,
                title = title,
                bio = bio,
                onUsernameChange = { username = it },
                onPhoneChange = { phone = it },
                onTitleChange = { title = it },
                onBioChange = { bio = it },
                onApply = {
                    // TODO: Implement profile save logic
                    onNavigateUp()
                },
                onBack = onNavigateUp
            )
        }
    }
}
