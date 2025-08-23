package feature.chat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.evmap.mobileapp.core.ui.model.ChatsUiState
import core.designsystem.AppTheme
import core.designsystem.Spacing
import core.ui.components.AppBottomBar
import core.ui.model.ChatUi
import feature.chat.ui.components.ChatRow



@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun ChatsScreenPreview() {
    // --- sample items (rename ChatItemUi to your actual item type) ---
    val sampleItems = listOf(
        ChatUi(
            id = "1",
            avatarUrl = null,
            name = "Alice",
            lastMessage = "See you soon!",
            lastMessageTime = "12:45",   // or Long/etc. – match your type
            unreadCount = 2
        ),
        ChatUi(
            id = "2",
            avatarUrl = null,
            name = "Bob",
            lastMessage = "Thanks!",
            lastMessageTime = "11:03",
            unreadCount = 0
        )
    )

    // --- sample UI state (rename fields to match your ChatsUiState) ---
    val ui = ChatsUiState(
        items = sampleItems,
        selection = setOf("2"),
        isSelecting = false
    )

    AppTheme {
        ChatsScreen(
            ui = ui,
            onOpenChat = {},
            onEnterSelectMode = {},
            onToggleSelect = {},
            onDeleteSelected = {},
            onExitSelectMode = {},
            currentRoute = "chats",
            onNavigate = {},
            modifier = Modifier
        )
    }
}

@Composable
fun ChatItemUi(
    id: String,
    avatarUrl: Nothing?,
    name: String,
    lastMessage: String,
    lastMessageTime: String,
    unreadCount: Int
) {
    TODO("Not yet implemented")
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(
    ui: ChatsUiState,
    onOpenChat: (String) -> Unit,
    onEnterSelectMode: () -> Unit,
    onToggleSelect: (String) -> Unit,
    onDeleteSelected: () -> Unit,
    onExitSelectMode: () -> Unit,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text("Chats")
                },
                navigationIcon = {
                    if (ui.isSelecting) {
                        IconButton(onClick = onExitSelectMode) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Exit selection mode"
                            )
                        }
                    }
                },
                actions = {
                    // Trash icon remains visible always
                    IconButton(onClick = onEnterSelectMode) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Select chats to delete"
                        )
                    }
                    
                    if (ui.isSelecting) {
                        IconButton(onClick = onExitSelectMode) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close selection mode"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            AppBottomBar(
                currentRoute = currentRoute,
                onNavigate = onNavigate
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Delete button bubble (sticky under app bar) - shown when in selection mode
            if (ui.isSelecting) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.m),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onDeleteSelected,
                            enabled = ui.selection.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            Text("Delete")
                        }
                    }
                }
            }

            // Chat list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(Spacing.m),
                verticalArrangement = Arrangement.spacedBy(Spacing.s)
            ) {
                items(ui.items) { item ->
                    ChatRow(
                        avatarUrl = item.avatarUrl,
                        name = item.name,
                        lastText = item.lastMessage ?: "",
                        time = item.lastMessageTime,
                        unreadCount = item.unreadCount,
                        selected = ui.selection.contains(item.id),
                        selectionMode = ui.isSelecting,
                        onClick = { 
                            if (ui.isSelecting) {
                                onToggleSelect(item.id)
                            } else {
                                onOpenChat(item.id)
                            }
                        },
                        onLongPress = { 
                            if (!ui.isSelecting) {
                                onEnterSelectMode()
                            }
                        },
                        onToggleSelect = { onToggleSelect(item.id) }
                    )
                }
            }
        }
    }
}
