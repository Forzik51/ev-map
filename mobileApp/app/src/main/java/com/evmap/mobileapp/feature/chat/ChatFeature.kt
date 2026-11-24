package feature.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.evmap.mobileapp.feature.chat.data.ChatMediaKind
import com.evmap.mobileapp.feature.chat.ui.ChatViewModel
import com.evmap.mobileapp.feature.chat.ui.ConversationScreen
import core.navigation.FeatureEntry
import feature.chat.ui.ChatsScreen

object ChatFeature : FeatureEntry {
    override val route = "chats"
    fun conversationRoute(chatId: Long) = "$route/$chatId"

    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        onNavigateToRoute: (String) -> Unit,
        onNavigateUp: () -> Unit,
    ) {
        navGraphBuilder.composable(route) {
            val viewModel: ChatViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.loadChats() }
            ChatsScreen(
                ui = state.chats,
                onOpenChat = { id -> id.toLongOrNull()?.let { onNavigateToRoute(conversationRoute(it)) } },
                onEnterSelectMode = viewModel::enterSelectMode,
                onToggleSelect = viewModel::toggleSelect,
                onDeleteSelected = viewModel::deleteSelected,
                onExitSelectMode = viewModel::exitSelectMode,
                currentRoute = route,
                onNavigate = onNavigateToRoute,
                loading = state.loading,
                error = state.error,
                onRetry = { viewModel.loadChats() },
            )
        }
        navGraphBuilder.composable("$route/{chatId}", arguments = listOf(navArgument("chatId") { type = NavType.LongType })) { entry ->
            val chatId = entry.arguments?.getLong("chatId") ?: return@composable
            ConversationRoute(chatId, onNavigateUp)
        }
    }
}

@Composable
private fun ConversationRoute(chatId: Long, onBack: () -> Unit) {
    val viewModel: ChatViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var choosing by rememberSaveable { mutableStateOf(false) }
    var pendingDownload by rememberSaveable { mutableStateOf<Long?>(null) }
    val photo = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.chooseAttachment(uri, ChatMediaKind.IMAGE)
    }
    val file = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.chooseAttachment(uri, ChatMediaKind.FILE)
    }
    val microphone = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        if (allowed) viewModel.startRecording()
        else viewModel.showError("Microphone permission is required to record a voice message.")
    }
    val saveFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val id = pendingDownload
        pendingDownload = null
        if (uri != null && id != null) viewModel.download(id, uri)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.resumeConversation() }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.pauseConversation() }
    DisposableEffect(viewModel) {
        onDispose { viewModel.pauseConversation() }
    }

    ConversationScreen(
        state = state,
        chatId = chatId,
        onBack = onBack,
        onInputChange = viewModel::updateInput,
        onSend = viewModel::send,
        onAttach = { choosing = true },
        onStartRecord = {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                viewModel.startRecording()
            } else microphone.launch(Manifest.permission.RECORD_AUDIO)
        },
        onStopRecord = viewModel::stopRecording,
        onCancelRecord = viewModel::cancelRecording,
        onDiscardAttachment = viewModel::discardAttachment,
        onOlder = viewModel::loadOlder,
        onVisibleIncoming = viewModel::visibleIncoming,
        onReconnect = viewModel::retryConnection,
        onAttachmentClick = { attachment ->
            if (attachment.kind == ChatMediaKind.VOICE.name) viewModel.toggleVoice(attachment)
            else if (pendingDownload == null) {
                pendingDownload = attachment.id
                saveFile.launch(attachment.name)
            }
        },
    )
    if (choosing) {
        AlertDialog(
            onDismissRequest = { choosing = false },
            title = { Text("Attach to chat") },
            text = {
                Column {
                    TextButton(onClick = {
                        choosing = false
                        photo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text("Photo") }
                    TextButton(onClick = {
                        choosing = false
                        file.launch(arrayOf("*/*"))
                    }) { Text("File") }
                }
            },
            confirmButton = { TextButton(onClick = { choosing = false }) { Text("Cancel") } },
        )
    }
}
