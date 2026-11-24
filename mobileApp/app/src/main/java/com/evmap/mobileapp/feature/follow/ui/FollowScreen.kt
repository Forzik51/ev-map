package feature.follow.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.evmap.mobileapp.feature.follow.ui.FollowList
import com.evmap.mobileapp.feature.follow.ui.FollowUiState
import core.designsystem.Spacing
import core.ui.components.AppBottomBar
import core.ui.components.UserListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowScreen(
    list: FollowList,
    state: FollowUiState,
    onUserClick: (String) -> Unit,
    onToggleFollow: (String) -> Unit,
    onRetry: () -> Unit,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(list.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = { AppBottomBar(currentRoute = "follow", onNavigate = onNavigate) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.loading || state.error != null || state.users.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.m),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    when {
                        state.loading -> CircularProgressIndicator()
                        state.error != null -> {
                            Text(state.error, color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = onRetry) { Text("Retry") }
                        }
                        list == FollowList.FOLLOWERS -> Text("No followers yet")
                        else -> Text("No followed users yet")
                    }
                }
            }
            LazyColumn(Modifier.weight(1f)) {
                items(state.users, key = { it.user.id }) { row ->
                    UserListItem(
                        avatarUrl = row.user.avatarUrl,
                        title = row.user.title ?: row.user.username,
                        subtitle = row.user.bio,
                        onClick = { onUserClick(row.user.id) },
                        trailing = {
                            if (state.viewerId != null && row.user.id != state.viewerId) {
                                val enabled = !state.loading && row.user.id !in state.changing
                                if (row.isFollowing) {
                                    OutlinedButton(onClick = { onToggleFollow(row.user.id) }, enabled = enabled) {
                                        Text("Following")
                                    }
                                } else {
                                    Button(onClick = { onToggleFollow(row.user.id) }, enabled = enabled) {
                                        Text("Follow")
                                    }
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}
