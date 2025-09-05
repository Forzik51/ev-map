package feature.saved.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import core.designsystem.AppTheme
import core.designsystem.Spacing
import core.ui.model.EventUi
import core.ui.model.GroupItemsUi
import core.ui.model.ViewMode
import feature.events.ui.components.EventCard
import feature.events.ui.components.EventGridCard

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun GroupItemsScreenPreview() {
    val sampleItems = listOf(
        EventUi(
            id = "1",
            title = "Title",
            location = "Subtitle",
            startsAt = "stAt",
            description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor",
            imageUrl = null,
            rating = 5.0f,
            reviewCount = 50
        ),
        EventUi(
            id = "2",
            title = "Title",
            location = "Subtitle",
            startsAt = "stAt",
            description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor",
            imageUrl = null,
            rating = 4.5f,
            reviewCount = 30
        )
    )

    val sampleUi = GroupItemsUi(
        groupId = "group1",
        title = "Name",
        items = sampleItems,
        viewMode = ViewMode.LIST
    )

    AppTheme {
        GroupItemsScreen(
            ui = sampleUi,
            onBack = {},
            onToggleView = {},
            onOpenItem = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupItemsScreen(
    ui: GroupItemsUi,
    onBack: () -> Unit,
    onToggleView: () -> Unit,
    onOpenItem: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
             TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Back",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = ui.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                },
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
                    // List view button (active when not in grid view)
                    IconButton(
                        onClick = { if (ui.viewMode == ViewMode.GRID) onToggleView() }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ViewList,
                            contentDescription = "List view",
                            tint = if (ui.viewMode == ViewMode.LIST) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }

                    // Grid view button (active when in grid view)
                    IconButton(
                        onClick = { if (ui.viewMode == ViewMode.LIST) onToggleView() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = "Grid view",
                            tint = if (ui.viewMode == ViewMode.GRID) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        if (ui.viewMode == ViewMode.GRID) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = Spacing.s),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                items(
                    items = ui.items,
                    key = { it.id }
                ) { event ->
                    EventGridCard(
                        event = event,
                        onClick = { onOpenItem(event.id) }
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = Spacing.m),
                verticalArrangement = Arrangement.spacedBy(Spacing.m)
            ) {
                items(
                    items = ui.items,
                    key = { it.id }
                ) { event ->
                    EventCard(
                        event = event,
                        onClick = onOpenItem
                    )
                }
            }
        }
    }
}
