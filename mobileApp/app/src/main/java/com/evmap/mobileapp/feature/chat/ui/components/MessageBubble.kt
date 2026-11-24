package feature.chat.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import core.designsystem.Spacing
import core.ui.model.MessageUi
import com.evmap.mobileapp.feature.chat.data.ChatMediaKind
import java.util.Locale

@Composable
fun MessageBubble(
    message: MessageUi,
    attachmentUrl: String?,
    playing: Boolean,
    onAttachmentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.m, vertical = Spacing.xs),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isMine) {
            // Other user's avatar for incoming messages
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (message.senderAvatarUrl.isNullOrBlank()) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = message.senderName?.firstOrNull()?.uppercaseChar()?.toString() ?: "A",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                } else {
                    AsyncImage(
                        model = message.senderAvatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(18.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(modifier = Modifier.width(Spacing.s))
        }

        // Message bubble
        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomStart = if (message.isMine) 20.dp else 8.dp,
                    bottomEnd = if (message.isMine) 8.dp else 20.dp
                ),
                color = if (message.isMine) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                }
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = Spacing.m,
                        vertical = Spacing.s
                    )
                ) {
                    val attachment = message.attachment
                    val contentColor = if (message.isMine) MaterialTheme.colorScheme.onSecondary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    if (attachment?.kind == ChatMediaKind.IMAGE.name) {
                        val context = LocalContext.current
                        val request = remember(context, attachmentUrl) {
                            ImageRequest.Builder(context)
                                .data(attachmentUrl)
                                .memoryCachePolicy(CachePolicy.DISABLED)
                                .diskCachePolicy(CachePolicy.DISABLED)
                                .build()
                        }
                        AsyncImage(
                            model = request,
                            contentDescription = attachment.name,
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClickLabel = "Save photo", onClick = onAttachmentClick),
                            contentScale = ContentScale.Crop
                        )
                    } else if (attachment != null) {
                        val label = if (attachment.kind == ChatMediaKind.VOICE.name) {
                            val seconds = (attachment.durationMs ?: 0) / 1000
                            val action = if (playing) "Stop voice message" else "Play voice message"
                            "$action · ${String.format(Locale.getDefault(), "%d:%02d", seconds / 60, seconds % 60)}"
                        } else {
                            "Download ${attachment.name}"
                        }
                        TextButton(
                            onClick = onAttachmentClick,
                            colors = ButtonDefaults.textButtonColors(contentColor = contentColor),
                        ) {
                            Text(label, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (!message.text.isNullOrBlank()) {
                        if (attachment != null) Spacer(Modifier.height(Spacing.xs))
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = contentColor,
                        )
                    }
                }
            }
            if (message.isMine) {
                Text(
                    if (message.isRead) "Read" else "Sent",
                    modifier = Modifier.padding(top = Spacing.xs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (message.isMine) {
            Spacer(modifier = Modifier.width(Spacing.s))
        }
    }
}
