package feature.map.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.evmap.mobileapp.feature.events.ui.displayDateTime
import com.evmap.mobileapp.feature.events.ui.displayRating
import core.designsystem.Spacing
import core.ui.model.EventUi

@Composable
fun MapEventPreviewCard(
    event: EventUi,
    onOpenEvent: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Card(onClick = { onOpenEvent(event.id) }, modifier = Modifier.widthIn(max = 280.dp)) {
        Column(Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(event.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Dismiss event preview") }
            }
            event.imageUrl?.let {
                AsyncImage(it, contentDescription = null, modifier = Modifier.fillMaxWidth().height(86.dp), contentScale = ContentScale.Crop)
            }
            Text(event.startsAt.displayDateTime(), style = MaterialTheme.typography.bodySmall)
            if (event.location.isNotBlank()) Text(event.location, style = MaterialTheme.typography.bodySmall)
            event.rating?.let { Text("★ ${it.displayRating()} (${event.reviewCount ?: 0})") }
            event.description?.takeIf(String::isNotBlank)?.let {
                Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
