package feature.profile.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import core.designsystem.Spacing

@Composable
fun ProfileHeader(
    username: String,
    followers: Int,
    following: Int,
    title: String?,
    bio: String?,
    isMe: Boolean,
    isFollowing: Boolean,
    onFollowToggle: () -> Unit,
    onMessage: () -> Unit,
    onOpenFollowers: () -> Unit,
    onOpenFollowing: () -> Unit,
    modifier: Modifier = Modifier,
    avatarUrl: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.m, vertical = Spacing.s)
    ) {
        // Profile info row with avatar and stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(94.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl.isNullOrBlank()) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                } else {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.width(Spacing.m))

            // Username and stats
            Column {
                // Username (only show for "me" profile since others show in top bar)
                if (isMe) {
                    Text(
                        text = username,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(Spacing.s))
                } else {
                    Spacer(modifier = Modifier.height(Spacing.m))
                }

                Row {
                    Column(
                        modifier = Modifier.clickable { onOpenFollowers() }
                    ) {
                        Text(
                            text = String.format("%03d", followers),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "followers",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(Spacing.xl))

                    Column(
                        modifier = Modifier.clickable { onOpenFollowing() }
                    ) {
                        Text(
                            text = String.format("%03d", following),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "following",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Bio section
        if (title != null || bio != null) {
            Spacer(modifier = Modifier.height(Spacing.m))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f)
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.m)
                ) {
                    if (title != null) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (bio != null) {
                        if (title != null) {
                            Spacer(modifier = Modifier.height(Spacing.xs))
                        }
                        Text(
                            text = bio,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Action buttons for other users
        if (!isMe) {
            Spacer(modifier = Modifier.height(Spacing.m))

            if (isFollowing) {
                // When following, show "Following" (outlined) + "Message" (filled) buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s)
                ) {
                    OutlinedButton(
                        onClick = onFollowToggle,
                        modifier = Modifier.weight(1f),
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

                    Button(
                        onClick = onMessage,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(
                            text = "Message",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            } else {
                // When not following, show single primary "Follow" button
                Button(
                    onClick = onFollowToggle,
                    modifier = Modifier.fillMaxWidth(),
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
    }
}
