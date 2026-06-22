package feature.follow

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.evmap.mobileapp.feature.follow.ui.FollowViewModel
import core.navigation.FeatureEntry
import feature.follow.ui.FollowersScreen
import feature.follow.ui.FollowingScreen

object FollowFeature : FeatureEntry {
    override val route = "follow"

    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        onNavigateToRoute: (String) -> Unit,
        onNavigateUp: () -> Unit
    ) {
        navGraphBuilder.composable(route = "profile/{userId}/followers") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            val viewModel: FollowViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()

            LaunchedEffect(userId) {
                viewModel.loadFollowers(userId)
            }

            FollowersScreen(
                users = state.users,
                onUserClick = { clickedUserId ->
                    onNavigateToRoute("profile/$clickedUserId")
                },
                onToggleFollow = viewModel::toggleFollow,
                onBack = onNavigateUp
            )
        }

        navGraphBuilder.composable(route = "profile/{userId}/following") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            val viewModel: FollowViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()

            LaunchedEffect(userId) {
                viewModel.loadFollowing(userId)
            }

            FollowingScreen(
                users = state.users,
                onUserClick = { clickedUserId ->
                    onNavigateToRoute("profile/$clickedUserId")
                },
                onToggleFollow = viewModel::toggleFollow,
                onBack = onNavigateUp
            )
        }
    }
}
