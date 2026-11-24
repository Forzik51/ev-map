package com.evmap.mobileapp.feature.events.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.LoadState
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.evmap.mobileapp.feature.events.data.EventsRepository
import com.evmap.mobileapp.feature.events.data.eventError
import com.evmap.mobileapp.feature.feed.data.toEventUi
import core.designsystem.Spacing
import dagger.hilt.android.lifecycle.HiltViewModel
import feature.events.ui.components.EventCard
import javax.inject.Inject

@HiltViewModel
class EventListViewModel @Inject constructor(repository: EventsRepository) : ViewModel() {
    val events = repository.catalog().cachedIn(viewModelScope)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventListScreen(
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onOpen: (Long) -> Unit,
    onSave: (String) -> Unit,
    viewModel: EventListViewModel = hiltViewModel(),
) {
    val events = viewModel.events.collectAsLazyPagingItems()
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("All events") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = onCreate) {
                    Icon(Icons.Default.Add, contentDescription = "Create event")
                }
            },
        )
    }) { padding ->
        if (events.itemCount == 0) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                when (val refresh = events.loadState.refresh) {
                    LoadState.Loading -> CircularProgressIndicator()
                    is LoadState.Error -> CatalogError(refresh.error.eventError(), events::retry)
                    is LoadState.NotLoading -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No events yet")
                        TextButton(onClick = onCreate) { Text("Create an event") }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(Spacing.m),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                val refresh = events.loadState.refresh
                if (refresh is LoadState.Error) {
                    item { CatalogError(refresh.error.eventError(), events::retry) }
                }
                items(events.itemCount, key = events.itemKey { it.id }) { index ->
                    events[index]?.let { event ->
                        EventCard(event = event.toEventUi(), onClick = { onOpen(event.id) }, onSave = onSave)
                    }
                }
                when (val append = events.loadState.append) {
                    LoadState.Loading -> item { CircularProgressIndicator(Modifier.padding(Spacing.m)) }
                    is LoadState.Error -> item { CatalogError(append.error.eventError(), events::retry) }
                    is LoadState.NotLoading -> Unit
                }
            }
        }
    }
}

@Composable
private fun CatalogError(message: String, onRetry: () -> Unit) {
    Column(Modifier.padding(Spacing.m), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message, color = MaterialTheme.colorScheme.error)
        TextButton(onClick = onRetry) { Text("Retry") }
    }
}
