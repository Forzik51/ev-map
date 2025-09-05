package feature.explore.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import core.designsystem.AppTheme
import core.designsystem.Spacing
import core.ui.components.SearchField
import core.ui.components.UserListItem
import core.ui.model.EventUi
import core.ui.model.ProfileUserUi
import feature.events.ui.components.EventCard
import feature.explore.ExploreTab
import feature.explore.ExploreUiState

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun ExploreScreenPreview() {
    // --- sample users ---
    val users = listOf(
        ProfileUserUi(
            id = "u1",
            username = "alice",
            title = "Alice Johnson",
            bio = "Coffee, cats, and Compose.",
            avatarUrl = null
        ),
        ProfileUserUi(
            id = "u2",
            username = "bob",
            title = "Bob Nowak",
            bio = "Runner • Android dev",
            avatarUrl = null
        )
    )

    // --- sample events ---
    val events = listOf(
        EventUi(
            id = "e1",
            title = "Open-Air Jazz",
            location = "Łazienki Park",
            startsAt = "startat",
            description = "Live bands, cozy vibes.",
            imageUrl = "https://picsum.photos/600/360?1",
            rating = 4.7F,
            reviewCount = 128
        ),
        EventUi(
            id = "e2",
            title = "Street Food Fest",
            location = "Praga-Północ",
            startsAt = "startat",
            description = "Food trucks and DJs.",
            imageUrl = "https://picsum.photos/600/360?2",
            rating = 4.5F,
            reviewCount = 89
        )
    )

    // --- preview state (mutable so tabs/search/filter react) ---
    var ui by remember {
        mutableStateOf(
            ExploreUiState(
                query = "",
                selectedTab = ExploreTab.EVENTS,
                users = users,
                events = events,
                isFilterOpen = false,
                sortBy = "Name",
                selectedType = "All"
            )
        )
    }

    AppTheme {
        ExploreScreen(
            uiState = ui,
            onQueryChange = { q -> ui = ui.copy(query = q) },
            onSelectTab = { tab -> ui = ui.copy(selectedTab = tab) },
            onUserClick = { /* no-op in preview */ },
            onEventClick = { /* no-op in preview */ },
            onOpenFilters = { ui = ui.copy(isFilterOpen = false) },
            onCloseFilters = { ui = ui.copy(isFilterOpen = false) },
            onApplyFilters = { ui = ui.copy(isFilterOpen = false) },
            onBack = { /* no-op in preview */ },
            onSave = { /* e.g., save event id */ },
            onSortChange = { sort -> ui = ui.copy(sortBy = sort) },
            onTypeChange = { type -> ui = ui.copy(selectedType = type) },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    uiState: ExploreUiState,
    onQueryChange: (String) -> Unit,
    onSelectTab: (ExploreTab) -> Unit,
    onUserClick: (String) -> Unit,
    onEventClick: (String) -> Unit,
    onOpenFilters: () -> Unit,
    onCloseFilters: () -> Unit,
    onApplyFilters: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onSave: ((String) -> Unit)? = null,
    onSortChange: ((String) -> Unit)? = null,
    onTypeChange: ((String) -> Unit)? = null
) {
    var isSearchActive by remember { mutableStateOf(false) }
    
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            if (isSearchActive) {
                // Inline search field
                SearchField(
                    value = uiState.query,
                    onValueChange = onQueryChange,
                    placeholder = "Input text",
                    modifier = Modifier.padding(Spacing.m)
                )
            } else {
                // Top app bar
                TopAppBar(
                    title = {
                        Text(
                            text = "Text",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    actions = {
                        // Search icon
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Filter icon
                        IconButton(onClick = onOpenFilters) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Filters",
                                tint = Color(0xFF464646)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab.ordinal]),
                        height = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                divider = {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            ) {
                Tab(
                    selected = uiState.selectedTab == ExploreTab.USERS,
                    onClick = { onSelectTab(ExploreTab.USERS) },
                    text = {
                        Text(
                            text = "Users",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                )
                Tab(
                    selected = uiState.selectedTab == ExploreTab.EVENTS,
                    onClick = { onSelectTab(ExploreTab.EVENTS) },
                    text = {
                        Text(
                            text = "Events",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                )
            }
            
            // Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.s)
            ) {
                when (uiState.selectedTab) {
                    ExploreTab.USERS -> {
                        items(uiState.users) { user ->
                            UserListItem(
                                avatarUrl = user.avatarUrl,
                                title = user.username,
                                subtitle = user.bio,
                                onClick = { onUserClick(user.id) }
                            )
                        }
                    }
                    ExploreTab.EVENTS -> {
                        items(uiState.events) { event ->
                            EventCard(
                                event = event,
                                onClick = onEventClick,
                                modifier = Modifier.padding(vertical = Spacing.s),
                                onOverflow = onSave
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Filter Sheet
    if (uiState.isFilterOpen) {
        ExploreFilterSheet(
            sortBy = uiState.sortBy,
            sortOptions = listOf("Name", "Date", "Rating", "Popularity"),
            type = uiState.selectedType,
            typeOptions = listOf("All", "Music", "Art", "Food", "Technology"),
            onSortChange = onSortChange ?: { },
            onTypeChange = onTypeChange ?: { },
            onApply = onApplyFilters,
            onDismiss = onCloseFilters
        )
    }
}
