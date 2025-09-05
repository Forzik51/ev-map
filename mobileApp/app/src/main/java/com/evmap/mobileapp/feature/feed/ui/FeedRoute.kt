package com.evmap.mobileapp.feature.feed.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import core.designsystem.Spacing
import core.ui.components.AppBottomBar
import core.ui.components.SearchField
import feature.events.ui.components.EventCard

@Composable
fun FeedRoute(
    currentRoute: String?,
    onOpenItem: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenCreate: () -> Unit,
    onNavigate: (String) -> Unit,
    vm: FeedViewModel = hiltViewModel()
) {
    val paging = vm.feed.collectAsLazyPagingItems()
    var query by remember { mutableStateOf("") }

    Scaffold(
        bottomBar = { AppBottomBar(currentRoute = currentRoute, onNavigate = onNavigate) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Hinted search text",
                onSearch = onOpenSearch,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.m, vertical = Spacing.m)
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = Spacing.m, vertical = Spacing.m),
                verticalArrangement = Arrangement.spacedBy(Spacing.m)
            ) {
                items(paging.itemCount) { index ->
                    val item = paging[index]
                    if (item != null) {
                        EventCard(
                            event = item.event,
                            onClick = { onOpenItem(item.event.id) }
                        )
                    }
                }
            }
        }
    }
}