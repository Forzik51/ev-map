package feature.saved.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.evmap.mobileapp.feature.saved.ui.SavedUiState
import core.designsystem.Spacing
import core.ui.model.EventUi
import core.ui.model.ViewMode
import feature.events.ui.components.EventCard
import feature.events.ui.components.EventGridCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupItemsScreen(
    state: SavedUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onToggleView: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onRemoveEvent: (String) -> Unit,
    onOpenItem: (String) -> Unit,
    onSaveItem: (String) -> Unit,
) {
    val group = state.groupItems
    var menuOpen by remember { mutableStateOf(false) }
    Scaffold(topBar = {
        TopAppBar(
            title = {
                Column {
                    Text(group?.title ?: "Saved list", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (group != null) Text(eventCountLabel(group.items.size), style = MaterialTheme.typography.bodySmall)
                }
            },
            navigationIcon = { SavedBackButton(onBack) },
            actions = {
                if (group != null) {
                    IconButton(onClick = onToggleView) {
                        if (group.viewMode == ViewMode.GRID) {
                            Icon(Icons.AutoMirrored.Outlined.List, contentDescription = "List view")
                        } else {
                            Icon(Icons.Outlined.GridView, contentDescription = "Grid view")
                        }
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }, enabled = !state.busy) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Manage list")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(text = { Text("Rename list") }, onClick = {
                                menuOpen = false
                                onRename()
                            })
                            DropdownMenuItem(text = { Text("Delete list") }, onClick = {
                                menuOpen = false
                                onDelete()
                            })
                        }
                    }
                }
            },
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SavedListStatus(state, "No events in this list".takeIf { group?.items?.isEmpty() == true }, onRetry)
            if (group != null && group.viewMode == ViewMode.GRID) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(Spacing.s),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(Spacing.s),
                ) {
                    items(group.items, key = { it.id }) { event ->
                        SavedEventCard(event, group.viewMode, state.busy, onOpenItem, onSaveItem, onRemoveEvent)
                    }
                }
            } else if (group != null) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(Spacing.m),
                    verticalArrangement = Arrangement.spacedBy(Spacing.m),
                ) {
                    items(group.items, key = { it.id }) { event ->
                        SavedEventCard(event, group.viewMode, state.busy, onOpenItem, onSaveItem, onRemoveEvent)
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedEventCard(
    event: EventUi,
    mode: ViewMode,
    busy: Boolean,
    onOpen: (String) -> Unit,
    onSave: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        if (mode == ViewMode.GRID) {
            EventGridCard(event, onClick = onOpen)
        } else {
            EventCard(
                event = event,
                onClick = onOpen,
                onOverflow = { if (!busy) menuOpen = true },
                onSave = onSave.takeUnless { busy },
            )
        }
        Box(Modifier.align(Alignment.TopEnd)) {
            if (mode == ViewMode.GRID) {
                Surface(shape = MaterialTheme.shapes.small) {
                    IconButton(onClick = { menuOpen = true }, enabled = !busy) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options for ${event.title}")
                    }
                }
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("Remove from list") }, enabled = !busy, onClick = {
                    menuOpen = false
                    onRemove(event.id)
                })
            }
        }
    }
}
