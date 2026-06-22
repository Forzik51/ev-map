package feature.profile

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.evmap.mobileapp.feature.profile.ui.ProfileViewModel
import core.navigation.FeatureEntry
import feature.profile.ui.ProfileScreen

object ProfileFeature : FeatureEntry {
    override val route = "profile"

    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        onNavigateToRoute: (String) -> Unit,
        onNavigateUp: () -> Unit
    ) {
        navGraphBuilder.composable(route) {
            val viewModel: ProfileViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()

            LaunchedEffect(Unit) {
                viewModel.loadMe()
            }

            ProfileScreen(
                ui = state,
                onFollowToggle = viewModel::toggleFollow,
                onMessage = {
                    state.user?.id?.let { onNavigateToRoute("chats") }
                },
                onOpenItem = { eventId ->
                    onNavigateToRoute("events/$eventId")
                },
                onOpenFollowers = {
                    onNavigateToRoute("profile/me/followers")
                },
                onOpenFollowing = {
                    onNavigateToRoute("profile/me/following")
                },
                onToggleView = viewModel::toggleView,
                onNavigateUp = onNavigateUp,
                currentRoute = route,
                onNavigate = onNavigateToRoute
            )
        }

        navGraphBuilder.composable(route = "profile/{userId}") { backStackEntry ->
            val rawUserId = backStackEntry.arguments?.getString("userId") ?: return@composable
            val userId = rawUserId.toLongOrNull()
            val viewModel: ProfileViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()

            LaunchedEffect(rawUserId) {
                if (userId == null) viewModel.loadMe() else viewModel.loadUser(userId)
            }

            ProfileScreen(
                ui = state,
                onFollowToggle = viewModel::toggleFollow,
                onMessage = {
                    state.user?.id?.let { onNavigateToRoute("chats") }
                },
                onOpenItem = { eventId ->
                    onNavigateToRoute("events/$eventId")
                },
                onOpenFollowers = {
                    onNavigateToRoute("profile/$rawUserId/followers")
                },
                onOpenFollowing = {
                    onNavigateToRoute("profile/$rawUserId/following")
                },
                onToggleView = viewModel::toggleView,
                onNavigateUp = onNavigateUp,
                currentRoute = route,
                onNavigate = onNavigateToRoute
            )
        }
    }
}
