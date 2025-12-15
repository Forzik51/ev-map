package feature.events.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.evmap.mobileapp.feature.events.ui.EventViewModel
import core.designsystem.Spacing
import core.ui.components.AppBottomBar
import feature.events.ui.components.MediaHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailsScreen(
    eventId: Long,
    //ui: EventDetailsUi,
    currentRoute: String?,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenOrganizer: () -> Unit,
    onOpenCategory: (String) -> Unit,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    vm: EventViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(eventId) {
        vm.load(eventId)
    }

    when {
        state.loading -> Text("Loading...")
        state.error != null -> Text("Error: ${state.error}")
        state.event != null -> Text("Title: ${state.event!!.description}")
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                    media = emptyList(),
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
                        text = "Error: ${state.error}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    state.event?.locationName.let { subtitle ->
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = subtitle ?: "none subtitle",
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
                        text = /*state.event.rating?.toString() ?:*/ "5.0",
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
                        text = /*state.event.reviewCount?.toString() ?:*/ "50",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.Black
                    )
                    
                    Spacer(modifier = Modifier.width(Spacing.m))
                    
                    // Views/Likes if present
                    state.event?.id.let { views ->
                        Text(
                            text = "$views views",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(Spacing.s))
                    }

                    state.event?.id.let { likes ->
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
                    state.event?.locationName.let { location ->
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
                                text = location.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    state.event?.startsAt.let { date ->
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
            /*if (state.event.categories.isNotEmpty()) {
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
            }*/
            
            // Long Description
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.m)
                ) {
                    val description = state.event?.description ?: "ui.event.description"
                    description.let {
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
