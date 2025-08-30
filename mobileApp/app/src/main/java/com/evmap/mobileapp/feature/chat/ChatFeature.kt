package feature.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import core.navigation.FeatureEntry
import core.ui.model.ChatUi
import core.ui.model.MessageUi
import feature.chat.ui.ChatsScreen
import com.evmap.mobileapp.core.ui.model.ChatsUiState
import com.evmap.mobileapp.feature.chat.ui.ConversationScreen
import com.evmap.mobileapp.core.ui.model.ConversationUi

object ChatFeature : FeatureEntry {
    override val route = "chats"

    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        onNavigateToRoute: (String) -> Unit,
        onNavigateUp: () -> Unit
    ) {
        // Chats list screen
        navGraphBuilder.composable(route = route) {
            var uiState by remember {
                mutableStateOf(
                    ChatsUiState(
                        items = getMockChats(),
                        isSelecting = false,
                        selection = emptySet()
                    )
                )
            }

            ChatsScreen(
                ui = uiState,
                onOpenChat = { chatId ->
                    onNavigateToRoute("chats/$chatId")
                },
                onEnterSelectMode = {
                    uiState = uiState.copy(isSelecting = true)
                },
                onToggleSelect = { chatId ->
                    val newSelection = if (uiState.selection.contains(chatId)) {
                        uiState.selection - chatId
                    } else {
                        uiState.selection + chatId
                    }
                    uiState = uiState.copy(selection = newSelection)
                },
                onDeleteSelected = {
                    val remainingItems = uiState.items.filterNot { chat ->
                        uiState.selection.contains(chat.id)
                    }
                    uiState = ChatsUiState(
                        items = remainingItems,
                        isSelecting = false,
                        selection = emptySet()
                    )
                },
                onExitSelectMode = {
                    uiState = uiState.copy(
                        isSelecting = false,
                        selection = emptySet()
                    )
                },
                currentRoute = route,
                onNavigate = onNavigateToRoute
            )
        }

        // Individual conversation screen
        navGraphBuilder.composable(route = "chats/{chatId}") { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable

            var conversationUi by remember {
                mutableStateOf(
                    ConversationUi(
                        messages = getMockMessages(),
                        input = "",
                        isRecording = false,
                        recordSeconds = 0
                    )
                )
            }

            // Get peer name from chat ID (in real app, fetch from repository)
            val peerName = getMockChats().find { it.id == chatId }?.name ?: "Unknown"

            ConversationScreen(
                ui = conversationUi,
                peerName = peerName,
                onBack = onNavigateUp,
                onInputChange = { newInput ->
                    conversationUi = conversationUi.copy(input = newInput)
                },
                onSend = {
                    if (conversationUi.input.isNotBlank()) {
                        // TODO: Implement send message logic
                        conversationUi = conversationUi.copy(input = "")
                    }
                },
                onAttach = {
                    // TODO: Implement attach media logic
                },
                onStartRecord = {
                    conversationUi = conversationUi.copy(isRecording = true, recordSeconds = 0)
                },
                onStopRecord = {
                    // TODO: Implement stop recording logic
                    conversationUi = conversationUi.copy(isRecording = false, recordSeconds = 0)
                },
                onCancelRecord = {
                    conversationUi = conversationUi.copy(isRecording = false, recordSeconds = 0)
                }
            )
        }
    }

    private fun getMockChats(): List<ChatUi> = listOf(
        ChatUi(
            id = "1",
            name = "Alice Johnson",
            lastMessage = "Hey, how are you doing?",
            lastMessageTime = "2:30 PM",
            unreadCount = 2
        ),
        ChatUi(
            id = "2",
            name = "Bob Smith",
            lastMessage = "Let's meet tomorrow",
            lastMessageTime = "1:15 PM",
            unreadCount = 0
        ),
        ChatUi(
            id = "3",
            name = "Carol Williams",
            lastMessage = "Thanks for your help!",
            lastMessageTime = "Yesterday",
            unreadCount = 1
        ),
        ChatUi(
            id = "4",
            name = "David Brown",
            lastMessage = "See you soon",
            lastMessageTime = "Yesterday",
            unreadCount = 0
        ),
        ChatUi(
            id = "5",
            name = "Emma Davis",
            lastMessage = "Great job on the project",
            lastMessageTime = "Monday",
            unreadCount = 5
        )
    )

    private fun getMockMessages(): List<MessageUi> = listOf(
        MessageUi(
            id = "1",
            text = "So excited!",
            timestamp = System.currentTimeMillis() - 86400000, // Yesterday
            isMine = false,
            senderName = "Alice",
            senderAvatarUrl = null
        ),
        MessageUi(
            id = "2",
            text = "What should we make?",
            timestamp = System.currentTimeMillis() - 86000000,
            isMine = false,
            senderName = "Alice",
            senderAvatarUrl = null
        ),
        MessageUi(
            id = "3",
            text = "Pasta?",
            timestamp = System.currentTimeMillis() - 85800000,
            isMine = false,
            senderName = "Alice",
            senderAvatarUrl = null
        ),
        MessageUi(
            id = "4",
            text = "or we could make this?",
            timestamp = System.currentTimeMillis() - 3600000,
            isMine = true,
            mediaUrl = "https://example.com/dumplings.jpg"
        ),
        MessageUi(
            id = "5",
            text = "Sounds good!",
            timestamp = System.currentTimeMillis() - 1800000,
            isMine = false,
            senderName = "Alice",
            senderAvatarUrl = null
        )
    )
}
