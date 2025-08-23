package feature.follow.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import core.designsystem.AppTheme
import core.designsystem.Spacing
import core.ui.components.UserListItem
import core.ui.model.ProfileUserUi
import core.ui.model.UserFollowUi
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import core.ui.components.AppBottomBar

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun FollowersScreenPreview() {
    val sampleUsers = listOf(
        UserFollowUi(
            user = ProfileUserUi(
                id = "1",
                username = "user1",
                title = "List item",
                bio = "Supporting line text lorem ipsum dolor sit amet, consectetur."
            ),
            isFollowing = false
        ),
        UserFollowUi(
            user = ProfileUserUi(
                id = "2", 
                username = "user2",
                title = "List item",
                bio = "Supporting line text lorem ipsum dolor sit amet, consectetur."
            ),
            isFollowing = true
        ),
        UserFollowUi(
            user = ProfileUserUi(
                id = "3",
                username = "user3", 
                title = "List item",
                bio = "Supporting line text lorem ipsum dolor sit amet, consectetur."
            ),
            isFollowing = false
        )
    )

    AppTheme {
        FollowersScreen(
            users = sampleUsers,
            onUserClick = {},
            onToggleFollow = {},
            onBack = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowersScreen(
    users: List<UserFollowUi>,
    onUserClick: (String) -> Unit,
    onToggleFollow: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            AppBottomBar(
                currentRoute = "profile",
                onNavigate = { /* TODO: Implement navigation */ }
            )
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Back",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Followers",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = Spacing.s)
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
                            AssistChip(
                                onClick = { onToggleFollow(userFollow.user.id) },
                                label = { Text("Followers", style = MaterialTheme.typography.labelLarge) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = Color.Transparent,
                                    labelColor = MaterialTheme.colorScheme.primary
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            )
                        } else {
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
