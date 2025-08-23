package feature.chat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import core.designsystem.Spacing
import core.ui.model.MessageUi

@Composable
fun MessageBubble(
    message: MessageUi,
    onClickMedia: ((String) -> Unit)? = null,
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
                    // Image content
                    if (!message.mediaUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = message.mediaUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { 
                                    onClickMedia?.invoke(message.mediaUrl)
                                },
                            contentScale = ContentScale.Crop
                        )
                        
                        if (!message.text.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(Spacing.xs))
                        }
                    }
                    
                    // Text content
                    if (!message.text.isNullOrBlank()) {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (message.isMine) {
                                MaterialTheme.colorScheme.onSecondary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }

        if (message.isMine) {
            Spacer(modifier = Modifier.width(Spacing.s))
        }
    }
}
