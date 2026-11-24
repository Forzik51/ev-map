package com.evmap.mobileapp.feature.feed.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.evmap.mobileapp.feature.events.data.eventError
import core.designsystem.Spacing
import core.ui.components.AppBottomBar
import core.ui.components.SearchField
import core.ui.model.FeedItemUi
import feature.events.ui.components.EventCard

@Composable
fun FeedRoute(
    currentRoute: String?,
    onOpenItem: (String) -> Unit,
    onSaveItem: (String) -> Unit,
    onOpenSearch: (String) -> Unit,
    onOpenCatalog: () -> Unit,
    onNavigate: (String) -> Unit,
    vm: FeedViewModel = hiltViewModel()
) {
    val paging = vm.feed.collectAsLazyPagingItems()
    var query by rememberSaveable { mutableStateOf("") }

    Scaffold(
        bottomBar = { AppBottomBar(currentRoute = currentRoute, onNavigate = onNavigate) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search events or users",
                onSearch = { onOpenSearch(query) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.m, vertical = Spacing.m)
            )
            TextButton(
                onClick = onOpenCatalog,
                modifier = Modifier.padding(horizontal = Spacing.m),
            ) {
                Text("All events")
            }
            FeedEvents(paging, onOpenItem, Modifier.weight(1f), onSaveItem)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FeedEvents(
    paging: LazyPagingItems<FeedItemUi>,
    onOpenItem: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSaveItem: ((String) -> Unit)? = null,
) {
    val listState = rememberLazyListState()
    val refresh = paging.loadState.refresh
    val hasEvents = paging.itemCount > 0
    var scrollAfterRefresh by remember { mutableStateOf(false) }

    LaunchedEffect(refresh, paging.itemSnapshotList) {
        if (scrollAfterRefresh && refresh is LoadState.NotLoading) {
            listState.requestScrollToItem(0)
            scrollAfterRefresh = false
        }
    }

    PullToRefreshBox(
        isRefreshing = hasEvents && refresh is LoadState.Loading,
        onRefresh = {
            if (refresh !is LoadState.Loading) {
                scrollAfterRefresh = true
                paging.refresh()
            }
        },
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.fillMaxSize()) {
            if (hasEvents && refresh is LoadState.Error) {
                FeedLoadError(refresh.error, paging::retry)
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(Spacing.m),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                if (!hasEvents) {
                    item {
                        Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            when (refresh) {
                                LoadState.Loading -> CircularProgressIndicator()
                                is LoadState.Error -> FeedLoadError(refresh.error, paging::retry)
                                is LoadState.NotLoading -> Text("No events yet")
                            }
                        }
                    }
                } else {
                    items(paging.itemCount, key = paging.itemKey { it.id }) { index ->
                        paging[index]?.let { item -> EventCard(event = item.event, onClick = onOpenItem, onSave = onSaveItem) }
                    }
                    when (val append = paging.loadState.append) {
                        LoadState.Loading -> item {
                            Box(Modifier.fillMaxWidth().padding(Spacing.m), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        is LoadState.Error -> item { FeedLoadError(append.error, paging::retry) }
                        is LoadState.NotLoading -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedLoadError(error: Throwable, onRetry: () -> Unit) {
    val message = remember(error) { error.eventError() }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.m, vertical = Spacing.s),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, color = MaterialTheme.colorScheme.error)
        TextButton(onClick = onRetry) { Text("Retry") }
    }
}
