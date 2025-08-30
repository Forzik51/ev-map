package feature.profile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import core.designsystem.AppTheme
import core.designsystem.Spacing
import core.ui.components.AppBottomBar
import core.ui.model.EventUi
import core.ui.model.ProfileUiState
import core.ui.model.ProfileUserUi
import feature.events.ui.components.EventCard
import feature.events.ui.components.EventGridCard
import feature.profile.ui.components.ProfileHeader
import feature.profile.ui.components.ProfileViewToggle

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun ProfileScreenPreview() {
    val sampleEvents = listOf(
        EventUi(
            id = "1",
            title = "Sample Event 1",
            subtitle = "Subtitle",
            description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor",
            imageUrl = null,
            rating = 5.0f,
            reviewCount = 50
        ),
        EventUi(
            id = "2",
            title = "Sample Event 2",
            subtitle = "Subtitle",
            description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor",
            imageUrl = null,
            rating = 4.5f,
            reviewCount = 32
        ),
        EventUi(
            id = "3",
            title = "Sample Event 3",
            subtitle = "Subtitle",
            description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor",
            imageUrl = null,
            rating = 4.8f,
            reviewCount = 18
        )
    )
    
    val sampleUser = ProfileUserUi(
        id = "user1",
        username = "Username",
        avatarUrl = null,
        title = "Title",
        bio = "Supporting line text lorem ipsum dolor sit amet, consectetur",
        followers = 123,
        following = 456
    )
    
    val sampleState = ProfileUiState(
        user = sampleUser,
        list = sampleEvents,
        isMe = false,
        isFollowing = true,
        selectedViewIsGrid = true // Preview grid mode
    )

    AppTheme {
        ProfileScreen(
            ui = sampleState,
            onFollowToggle = {},
            onMessage = {},
            onOpenItem = {},
            onOpenFollowers = {},
            onOpenFollowing = {},
            onToggleView = {},
            onNavigateUp = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    ui: ProfileUiState,
    onFollowToggle: () -> Unit,
    onMessage: () -> Unit,
    onOpenItem: (String) -> Unit,
    onOpenFollowers: () -> Unit,
    onOpenFollowing: () -> Unit,
    onToggleView: () -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (!ui.isMe) {
                TopAppBar(
                    title = { 
                        Text(
                            text = ui.user?.username ?: "",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateUp) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
            }
        },
        bottomBar = {
            AppBottomBar(
                currentRoute = "profile",
                onNavigate = { /* TODO: Implement navigation */ }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        if (ui.user != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Profile Header - always at top
                ProfileHeader(
                    username = ui.user.username,
                    followers = ui.user.followers,
                    following = ui.user.following,
                    title = ui.user.title,
                    bio = ui.user.bio,
                    isMe = ui.isMe,
                    isFollowing = ui.isFollowing,
                    onFollowToggle = onFollowToggle,
                    onMessage = onMessage,
                    onOpenFollowers = onOpenFollowers,
                    onOpenFollowing = onOpenFollowing,
                    avatarUrl = ui.user.avatarUrl,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // View Toggle - uses ui.selectedViewIsGrid directly
                ProfileViewToggle(
                    isGrid = ui.selectedViewIsGrid,
                    onToggle = onToggleView,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Events Content - Grid or List based on ui.selectedViewIsGrid
                if (ui.selectedViewIsGrid) {
                    // Grid mode - LazyVerticalGrid with 3 columns (matching design frame)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = Spacing.s),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        items(
                            items = ui.list,
                            key = { it.id }
                        ) { event ->
                            EventGridCard(
                                event = event,
                                onClick = { onOpenItem(event.id) }
                            )
                        }
                    }
                } else {
                    // List mode - LazyColumn with EventCard
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = Spacing.s),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(
                            items = ui.list,
                            key = { it.id }
                        ) { event ->
                            EventCard(
                                event = event,
                                onClick = { onOpenItem(event.id) }
                            )
                        }
                    }
                }
            }
        } else if (ui.isLoading) {
            // Loading state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else if (ui.error != null) {
            // Error state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Error",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(Spacing.s))
                    Text(
                        text = ui.error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
