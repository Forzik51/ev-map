package feature.profile

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.paging.compose.collectAsLazyPagingItems
import com.evmap.mobileapp.feature.profile.ui.ProfileViewModel
import core.navigation.FeatureEntry
import feature.chat.ChatFeature
import feature.profile.ui.ProfileScreen
import feature.saved.SavedFeature

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

            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                viewModel.loadMe()
            }

            ProfileScreen(
                ui = state,
                events = viewModel.events.collectAsLazyPagingItems(),
                onFollowToggle = viewModel::toggleFollow,
                onMessage = { viewModel.openChat() },
                onOpenItem = { eventId ->
                    onNavigateToRoute("events/$eventId")
                },
                onSaveItem = { onNavigateToRoute(SavedFeature.pickerRoute(it)) },
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

            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                if (userId == null) viewModel.loadMe() else viewModel.loadUser(userId)
            }
            LaunchedEffect(state.openedChatId) {
                state.openedChatId?.let { chatId ->
                    viewModel.consumeOpenedChat()
                    onNavigateToRoute(ChatFeature.conversationRoute(chatId))
                }
            }

            ProfileScreen(
                ui = state,
                events = viewModel.events.collectAsLazyPagingItems(),
                onFollowToggle = viewModel::toggleFollow,
                onMessage = { viewModel.openChat() },
                onOpenItem = { eventId ->
                    onNavigateToRoute("events/$eventId")
                },
                onSaveItem = { onNavigateToRoute(SavedFeature.pickerRoute(it)) },
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
