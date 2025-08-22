package feature.events.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import core.designsystem.AppTheme
import core.designsystem.Spacing
import core.ui.components.AppBottomBar
import core.ui.components.CategoryChip
import core.ui.components.ChipsFlowRow
import core.ui.model.EventDetailsUi
import core.ui.model.EventUi
import feature.events.ui.components.MediaHeader



@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun EventDetailsScreenPreview() {
    // Minimal sample EventUi – adjust if your EventUi has more required fields
    val sampleEvent = EventUi(
        id = "e1",
        title = "Jazz in the Park",
        subtitle = "Łazienki Królewskie",
        description = "An evening of live jazz under the stars.",
        imageUrl = "https://picsum.photos/800/400",
        rating = 4.7F,
        reviewCount = 256
    )

    val sampleUi = EventDetailsUi(
        event = sampleEvent,
        gallery = listOf(
            "https://picsum.photos/900/600?1",
            "https://picsum.photos/900/600?2",
            "https://picsum.photos/900/600?3"
        ),
        fullDescription = "Longer description for preview purposes. " +
                "Bring a blanket and enjoy live performances from local artists.",
        location = "Warsaw, Poland",
        date = "Aug 24, 2025 • 19:00",
        categories = listOf("Music", "Outdoor", "Family"),
        viewCount = 1342,
        likeCount = 240,
        organizerName = "City of Warsaw",
        organizerId = "org_waw"
    )

    AppTheme {
        EventDetailsScreen(
            ui = sampleUi,
            currentRoute = "events",
            onBack = {},
            onShare = {},
            onOpenMap = {},
            onOpenOrganizer = {},
            onOpenCategory = {},
            onNavigate = {},
            modifier = Modifier
        )
    }
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailsScreen(
    ui: EventDetailsUi,
    currentRoute: String?,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenOrganizer: () -> Unit,
    onOpenCategory: (String) -> Unit,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onShare) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        bottomBar = {
            AppBottomBar(
                currentRoute = currentRoute,
                onNavigate = onNavigate
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(Spacing.m)
        ) {
            // Media Header (read-only gallery)
            item {
                MediaHeader(
                    media = ui.gallery,
                    onAddClick = null, // Read-only, no add functionality
                    onRemoveAt = null, // Read-only, no remove functionality
                    modifier = Modifier.padding(horizontal = Spacing.s)
                )
            }
            
            // Title and Subtitle
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.m)
                ) {
                    Text(
                        text = ui.event.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    ui.event.subtitle?.let { subtitle ->
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            // Rating Row + Meta Icons
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.m),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rating
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        modifier = Modifier.size(32.dp),
                        tint = Color(0xFF464646)
                    )
                    
                    Spacer(modifier = Modifier.width(Spacing.s))
                    
                    Text(
                        text = ui.event.rating?.toString() ?: "5.0",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.Black
                    )
                    
                    Spacer(modifier = Modifier.width(Spacing.s))
                    
                    // Comment/Add icon
                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape = MaterialTheme.shapes.large,
                        color = Color(0xFF464646)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = "Add comment",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(Spacing.s))
                    
                    Text(
                        text = ui.event.reviewCount?.toString() ?: "50",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.Black
                    )
                    
                    Spacer(modifier = Modifier.width(Spacing.m))
                    
                    // Views/Likes if present
                    ui.viewCount?.let { views ->
                        Text(
                            text = "$views views",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(Spacing.s))
                    }
                    
                    ui.likeCount?.let { likes ->
                        Text(
                            text = "$likes likes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Location and Date
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.m)
                ) {
                    ui.location?.let { location ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Location",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            Text(
                                text = location,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    ui.date?.let { date ->
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = "Date $date",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Categories
            if (ui.categories.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.m)
                    ) {
                        Text(
                            text = "Categories",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(Spacing.s))
                        
                        ChipsFlowRow {
                            ui.categories.forEach { category ->
                                CategoryChip(
                                    text = category,
                                    selected = false,
                                    onToggle = { onOpenCategory(category) }
                                )
                            }
                        }
                    }
                }
            }
            
            // Long Description
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.m)
                ) {
                    val description = ui.fullDescription ?: ui.event.description
                    description?.let {
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(Spacing.s))
                        
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Bottom spacing for navigation bar
            item {
                Spacer(modifier = Modifier.height(Spacing.m))
            }
        }
    }
}
