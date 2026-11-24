package feature.follow

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.evmap.mobileapp.feature.follow.ui.FollowList
import com.evmap.mobileapp.feature.follow.ui.FollowViewModel
import core.navigation.FeatureEntry
import feature.follow.ui.FollowScreen

object FollowFeature : FeatureEntry {
    override val route = "follow"

    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        onNavigateToRoute: (String) -> Unit,
        onNavigateUp: () -> Unit,
    ) {
        FollowList.entries.forEach { list ->
            navGraphBuilder.composable(route = "profile/{userId}/${list.segment}") { entry ->
                val userId = entry.arguments?.getString("userId") ?: return@composable
                val viewModel: FollowViewModel = hiltViewModel()
                val state by viewModel.state.collectAsStateWithLifecycle()

                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load(userId, list) }

                FollowScreen(
                    list = list,
                    state = state,
                    onUserClick = { onNavigateToRoute("profile/$it") },
                    onToggleFollow = viewModel::toggleFollow,
                    onRetry = { viewModel.load(userId, list) },
                    onNavigate = onNavigateToRoute,
                    onBack = onNavigateUp,
                )
            }
        }
    }
}
