package feature.events.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import core.designsystem.Spacing

@Composable
fun MediaHeader(
    media: List<String>,
    onAddClick: (() -> Unit)? = null,
    onRemoveAt: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Show at least 2 placeholder slots when empty for better UX
    val displayMedia = if (media.isEmpty()) {
        listOf("placeholder1", "placeholder2")
    } else {
        media
    }
    
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .height(151.dp)
            .padding(horizontal = Spacing.s),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        contentPadding = PaddingValues(vertical = Spacing.s)
    ) {
        // Media items
        itemsIndexed(displayMedia) { index, imageUrl ->
            MediaSlot(
                imageUrl = if (imageUrl.startsWith("placeholder")) null else imageUrl,
                index = index,
                onRemove = if (onRemoveAt != null && !imageUrl.startsWith("placeholder")) {
                    { onRemoveAt(index) }
                } else null
            )
        }
        
        // Add button - always show as separate item
        if (onAddClick != null) {
            item {
                AddMediaButton(
                    onClick = onAddClick,
                    modifier = Modifier.height(135.dp)
                )
            }
        }
    }
}

@Composable
private fun MediaSlot(
    imageUrl: String?,
    index: Int,
    onRemove: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(168.dp)
            .height(135.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFFE0E0E0))
    ) {
        if (imageUrl != null) {
            // Real image
            AsyncImage(
                model = imageUrl,
                contentDescription = "Event image ${index + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // Remove button overlay
            if (onRemove != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Spacing.s),
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.7f)
                ) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove image",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        } else {
            // Placeholder content matching the design
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Triangle shape placeholder
                Box(
                    modifier = Modifier
                        .size(width = 48.dp, height = 40.dp)
                        .background(
                            color = Color(0xFF9E9E9E),
                            shape = RoundedCornerShape(4.dp)
                        )
                )
                
                Spacer(modifier = Modifier.height(Spacing.s))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Square placeholder
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = Color(0xFF9E9E9E),
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                    
                    // Circle with number
                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape = CircleShape,
                        color = Color(0xFF9E9E9E)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (index) {
                                    0 -> "1st"
                                    1 -> "2nd"
                                    2 -> "3rd"
                                    else -> "${index + 1}th"
                                },
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddMediaButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .width(44.dp)
            .clip(RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF4C4C4C)
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add photo",
                tint = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
