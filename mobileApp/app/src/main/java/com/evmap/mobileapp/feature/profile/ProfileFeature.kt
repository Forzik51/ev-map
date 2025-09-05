package feature.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import core.navigation.FeatureEntry
import core.ui.model.EventUi
import core.ui.model.FeedItemUi
import core.ui.model.ProfileUiState
import core.ui.model.ProfileUserUi
import feature.map.MapFeature
import feature.profile.ui.ProfileScreen

object ProfileFeature : FeatureEntry {
    override val route = "profile"

    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        onNavigateToRoute: (String) -> Unit,
        onNavigateUp: () -> Unit
    ) {
        // My Profile
        navGraphBuilder.composable(route) {backStackEntry ->
            val userId = "me"
            var selectedViewIsGrid by remember { mutableStateOf(false) }

                val e1 = EventUi(
                    id = "1",
                    title = "Sample Event 1",
                    location = "Event subtitle",
                    startsAt = "stAt",
                    description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor",
                    imageUrl = null,
                    rating = 5.0f,
                    reviewCount = 50
                )
            val e2 = EventUi(
                    id = "2",
                    title = "Sample Event 2",
                    location = "Another subtitle",
                    startsAt = "stAt",
                    description = "Another description here",
                    imageUrl = null,
                    rating = 4.8f,
                    reviewCount = 32
                )

            val sampleItems = listOf(
                FeedItemUi(id = "1", event = e1, timestamp = System.currentTimeMillis(), isPromoted = true),
                FeedItemUi(id = "2", event = e2),
            )

            val sampleUser = ProfileUserUi(
                id = "me",
                username = "username",
                avatarUrl = null,
                title = "My Title",
                bio = "Supporting line text lorem ipsum dolor sit amet, consectetur",
                followers = 123,
                following = 456
            )

            val uiState = ProfileUiState(
                user = sampleUser,
                list = sampleItems,
                isMe = true,
                isFollowing = false,
                selectedViewIsGrid = selectedViewIsGrid,
                isLoading = false
            )

            ProfileScreen(
                ui = uiState,
                onFollowToggle = {
                    // TODO: Implement follow logic
                },
                onMessage = {
                    // TODO: Implement message logic
                },
                onOpenItem = { eventId ->
                    onNavigateToRoute("events/$eventId")
                },
                onOpenFollowers = {
                    onNavigateToRoute("profile/$userId/followers")
                },
                onOpenFollowing = {
                    onNavigateToRoute("profile/$userId/following")
                },
                onToggleView = {
                    selectedViewIsGrid = !selectedViewIsGrid
                },
                onNavigateUp = onNavigateUp,
                currentRoute = MapFeature.route,
                onNavigate = onNavigateToRoute
            )
        }

        // Other User Profile
        navGraphBuilder.composable(route = "profile/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable

            // TODO: Replace with actual ViewModel when available
            var isFollowing by remember { mutableStateOf(false) }
            var selectedViewIsGrid by remember { mutableStateOf(false) }
            

                val e1 = EventUi(
                    id = "1",
                    title = "User Event 1", 
                    location = "Event subtitle",
                    startsAt = "stAt",
                    description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit",
                    imageUrl = null,
                    rating = 4.5f,
                    reviewCount = 25
                )

            val sampleItems = listOf(
                FeedItemUi(id = "1", event = e1, timestamp = System.currentTimeMillis(), isPromoted = true),
            )
            
            val sampleUser = ProfileUserUi(
                id = userId,
                username = "otheruser",
                avatarUrl = null,
                title = "User Title",
                bio = "User bio text here",
                followers = 98,
                following = 76
            )
            
            val uiState = ProfileUiState(
                user = sampleUser,
                list = sampleItems,
                isMe = false,
                isFollowing = isFollowing,
                selectedViewIsGrid = selectedViewIsGrid,
                isLoading = false
            )

            ProfileScreen(
                ui = uiState,
                onFollowToggle = {
                    isFollowing = !isFollowing
                },
                onMessage = {
                    // TODO: Implement message logic
                },
                onOpenItem = { eventId ->
                    onNavigateToRoute("events/$eventId")
                },
                onOpenFollowers = {
                    onNavigateToRoute("profile/$userId/followers")
                },
                onOpenFollowing = {
                    onNavigateToRoute("profile/$userId/following")
                },
                onToggleView = {
                    selectedViewIsGrid = !selectedViewIsGrid
                },
                onNavigateUp = onNavigateUp,
                currentRoute = MapFeature.route,
                onNavigate = onNavigateToRoute
            )
        }
    }
}
