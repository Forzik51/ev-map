package feature.follow.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import core.designsystem.AppTheme
import core.ui.components.AppBottomBar
import core.ui.components.UserListItem
import core.ui.model.ProfileUserUi
import core.ui.model.UserFollowUi

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun FollowingScreenPreview() {
    val sampleUsers = listOf(
        UserFollowUi(
            user = ProfileUserUi(
                id = "user1",
                username = "List item",
                title = "List item",
                bio = "Supporting line text lorem ipsum dolor sit amet, consectetur.",
                avatarUrl = null
            ),
            isFollowing = false
        ),
        UserFollowUi(
            user = ProfileUserUi(
                id = "user2",
                username = "List item",
                title = "List item", 
                bio = "Supporting line text lorem ipsum dolor sit amet, consectetur.",
                avatarUrl = null
            ),
            isFollowing = true
        ),
        UserFollowUi(
            user = ProfileUserUi(
                id = "user3",
                username = "List item",
                title = "List item",
                bio = "Supporting line text lorem ipsum dolor sit amet, consectetur.",
                avatarUrl = null
            ),
            isFollowing = false
        ),
        UserFollowUi(
            user = ProfileUserUi(
                id = "user4",
                username = "List item",
                title = "List item",
                bio = "Supporting line text lorem ipsum dolor sit amet, consectetur.",
                avatarUrl = null
            ),
            isFollowing = true
        )
    )

    AppTheme {
        FollowingScreen(
            users = sampleUsers,
            onUserClick = {},
            onToggleFollow = {},
            onBack = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowingScreen(
    users: List<UserFollowUi>,
    onUserClick: (String) -> Unit,
    onToggleFollow: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Following",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            AppBottomBar(
                currentRoute = "follow",
                onNavigate = { /* TODO: Implement navigation */ }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(
                items = users,
                key = { it.user.id }
            ) { userFollow ->
                UserListItem(
                    avatarUrl = userFollow.user.avatarUrl,
                    title = userFollow.user.title ?: userFollow.user.username,
                    subtitle = userFollow.user.bio,
                    onClick = { onUserClick(userFollow.user.id) },
                    trailing = {
                        if (userFollow.isFollowing) {
                            // Following - Outlined button
                            OutlinedButton(
                                onClick = { onToggleFollow(userFollow.user.id) },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Text(
                                    text = "Following",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        } else {
                            // Follow - Filled button
                            Button(
                                onClick = { onToggleFollow(userFollow.user.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Text(
                                    text = "Follow",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}
