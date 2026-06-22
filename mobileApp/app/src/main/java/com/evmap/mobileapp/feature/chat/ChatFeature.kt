package feature.chat

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.evmap.mobileapp.feature.chat.ui.ChatViewModel
import com.evmap.mobileapp.feature.chat.ui.ConversationScreen
import core.navigation.FeatureEntry
import feature.chat.ui.ChatsScreen

object ChatFeature : FeatureEntry {
    override val route = "chats"

    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        onNavigateToRoute: (String) -> Unit,
        onNavigateUp: () -> Unit
    ) {
        navGraphBuilder.composable(route = route) {
            val viewModel: ChatViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()

            LaunchedEffect(Unit) {
                viewModel.loadChats()
            }

            ChatsScreen(
                ui = state.chats,
                onOpenChat = { chatId ->
                    onNavigateToRoute("chats/$chatId")
                },
                onEnterSelectMode = viewModel::enterSelectMode,
                onToggleSelect = viewModel::toggleSelect,
                onDeleteSelected = viewModel::deleteSelected,
                onExitSelectMode = viewModel::exitSelectMode,
                currentRoute = route,
                onNavigate = onNavigateToRoute
            )
        }

        navGraphBuilder.composable(route = "chats/{chatId}") { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
            val viewModel: ChatViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()

            LaunchedEffect(chatId) {
                viewModel.loadConversation(chatId)
            }

            ConversationScreen(
                ui = state.conversation,
                peerName = state.peerName,
                onBack = onNavigateUp,
                onInputChange = viewModel::updateInput,
                onSend = { viewModel.send(chatId) },
                onAttach = {
                    // Media attachments are pending backend contract/UI picker work.
                },
                onStartRecord = {
                    // Voice recording remains UI-only until backend media messages exist.
                },
                onStopRecord = {},
                onCancelRecord = {}
            )
        }
    }
}
