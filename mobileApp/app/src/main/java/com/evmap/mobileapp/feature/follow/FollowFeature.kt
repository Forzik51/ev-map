package feature.follow

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import core.navigation.FeatureEntry
import core.ui.model.ProfileUserUi
import core.ui.model.UserFollowUi
import feature.follow.ui.FollowersScreen
import feature.follow.ui.FollowingScreen

object FollowFeature : FeatureEntry {
    override val route = "follow"

    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        onNavigateToRoute: (String) -> Unit,
        onNavigateUp: () -> Unit
    ) {
        // Followers Screen
        navGraphBuilder.composable(route = "profile/{userId}/followers") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            
            // TODO: Replace with actual ViewModel when available
            var followersState by remember { 
                mutableStateOf(
                    listOf(
                        UserFollowUi(
                            user = ProfileUserUi(
                                id = "follower1",
                                username = "follower1",
                                title = "List item",
                                bio = "Supporting line text lorem ipsum dolor sit amet, consectetur.",
                                avatarUrl = null
                            ),
                            isFollowing = false
                        ),
                        UserFollowUi(
                            user = ProfileUserUi(
                                id = "follower2",
                                username = "follower2", 
                                title = "List item",
                                bio = "Supporting line text lorem ipsum dolor sit amet, consectetur.",
                                avatarUrl = null
                            ),
                            isFollowing = true
                        ),
                        UserFollowUi(
                            user = ProfileUserUi(
                                id = "follower3",
                                username = "follower3",
                                title = "List item", 
                                bio = "Supporting line text lorem ipsum dolor sit amet, consectetur.",
                                avatarUrl = null
                            ),
                            isFollowing = false
                        ),
                        UserFollowUi(
                            user = ProfileUserUi(
                                id = "follower4",
                                username = "follower4",
                                title = "List item",
                                bio = "Supporting line text lorem ipsum dolor sit amet, consectetur.",
                                avatarUrl = null
                            ),
                            isFollowing = true
                        )
                    )
                )
            }

            FollowersScreen(
                users = followersState,
                onUserClick = { clickedUserId ->
                    onNavigateToRoute("profile/$clickedUserId")
                },
                onToggleFollow = { toggleUserId ->
                    followersState = followersState.map { userFollow ->
                        if (userFollow.user.id == toggleUserId) {
                            userFollow.copy(isFollowing = !userFollow.isFollowing)
                        } else {
                            userFollow
                        }
                    }
                },
                onBack = onNavigateUp
            )
        }

        // Following Screen
        navGraphBuilder.composable(route = "profile/{userId}/following") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable

            // TODO: Replace with actual ViewModel when available
            var followingState by remember {
                mutableStateOf(
                    listOf(
                        UserFollowUi(
                            user = ProfileUserUi(
                                id = "following1",
                                username = "following1",
                                title = "List item",
                                bio = "Supporting line text lorem ipsum dolor sit amet, consectetur.",
                                avatarUrl = null
                            ),
                            isFollowing = false
                        ),
                        UserFollowUi(
                            user = ProfileUserUi(
                                id = "following2",
                                username = "following2",
                                title = "List item",
                                bio = "Supporting line text lorem ipsum dolor sit amet, consectetur.",
                                avatarUrl = null
                            ),
                            isFollowing = true
                        ),
                        UserFollowUi(
                            user = ProfileUserUi(
                                id = "following3",
                                username = "following3",
                                title = "List item",
                                bio = "Supporting line text lorem ipsum dolor sit amet, consectetur.",
                                avatarUrl = null
                            ),
                            isFollowing = false
                        ),
                        UserFollowUi(
                            user = ProfileUserUi(
                                id = "following4",
                                username = "following4",
                                title = "List item",
                                bio = "Supporting line text lorem ipsum dolor sit amet, consectetur.",
                                avatarUrl = null
                            ),
                            isFollowing = true
                        )
                    )
                )
            }

            FollowingScreen(
                users = followingState,
                onUserClick = { clickedUserId ->
                    onNavigateToRoute("profile/$clickedUserId")
                },
                onToggleFollow = { toggleUserId ->
                    followingState = followingState.map { userFollow ->
                        if (userFollow.user.id == toggleUserId) {
                            userFollow.copy(isFollowing = !userFollow.isFollowing)
                        } else {
                            userFollow
                        }
                    }
                },
                onBack = onNavigateUp
            )
        }
    }
}
