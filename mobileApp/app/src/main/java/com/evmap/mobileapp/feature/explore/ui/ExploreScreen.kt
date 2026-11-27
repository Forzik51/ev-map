package feature.explore.ui
import androidx.compose.ui.res.stringResource
import com.evmap.mobileapp.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import core.designsystem.AppTheme
import core.designsystem.Spacing
import core.ui.components.SearchField
import core.ui.components.UserListItem
import feature.events.ui.components.EventCard
import feature.explore.ExploreSort
import feature.explore.ExploreTab
import feature.explore.ExploreUiState

@Composable
fun ExploreScreen(
    uiState: ExploreUiState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onSelectTab: (ExploreTab) -> Unit,
    onUserClick: (String) -> Unit,
    onEventClick: (String) -> Unit,
    onOpenFilters: () -> Unit,
    onCloseFilters: () -> Unit,
    onApplyFilters: () -> Unit,
    onSortChange: (ExploreSort) -> Unit,
    onCategoryChange: (Long?) -> Unit,
    onTagsChange: (String)->Unit = {},
    onRetryCategories: () -> Unit,
    onBack: () -> Unit,
    onSaveEvent: ((String) -> Unit)? = null,
) {
    val focus = LocalFocusManager.current
    Scaffold(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.s, horizontal = Spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.form_back_b52b36))
                }
                SearchField(
                    value = uiState.query,
                    onValueChange = onQueryChange,
                    placeholder = stringResource(R.string.form_search_events_or_users_0705f6),
                    modifier = Modifier.weight(1f),
                    onSearch = {
                        focus.clearFocus()
                        onSearch()
                    },
                )
                if (uiState.selectedTab == ExploreTab.EVENTS) {
                    IconButton(onClick = {
                        focus.clearFocus()
                        onOpenFilters()
                    }) {
                        Icon(Icons.Outlined.Tune, contentDescription = stringResource(R.string.form_filters_96e578))
                    }
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
                Tab(
                    selected = uiState.selectedTab == ExploreTab.USERS,
                    onClick = { onSelectTab(ExploreTab.USERS) },
                    text = { Text(stringResource(R.string.form_users_57f2b1)) },
                )
                Tab(
                    selected = uiState.selectedTab == ExploreTab.EVENTS,
                    onClick = { onSelectTab(ExploreTab.EVENTS) },
                    text = { Text(stringResource(R.string.form_events_c5497b)) },
                )
            }

            val empty = when (uiState.selectedTab) {
                ExploreTab.USERS -> uiState.users.isEmpty()
                ExploreTab.EVENTS -> uiState.events.isEmpty()
            }
            val error = uiState.error
            when {
                uiState.query.isBlank() -> SearchStatus { Text(stringResource(R.string.form_enter_a_search_term_9e6ace)) }
                uiState.loading -> SearchStatus { CircularProgressIndicator() }
                error != null -> SearchStatus {
                    Text(error, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onSearch) { Text(stringResource(R.string.form_retry_9f5cd8)) }
                }
                empty -> SearchStatus { Text(stringResource(R.string.form_no_results_found_658e79)) }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = Spacing.s),
                    verticalArrangement = Arrangement.spacedBy(Spacing.s),
                ) {
                    when (uiState.selectedTab) {
                        ExploreTab.USERS -> items(uiState.users, key = { it.id }) { user ->
                            UserListItem(
                                avatarUrl = user.avatarUrl,
                                title = user.username,
                                subtitle = user.bio,
                                onClick = { onUserClick(user.id) },
                            )
                        }
                        ExploreTab.EVENTS -> items(uiState.events, key = { it.id }) { event ->
                            EventCard(event = event, onClick = onEventClick, onSave = onSaveEvent)
                        }
                    }
                }
            }
        }
    }

    uiState.draftFilters?.let { filters ->
        ExploreFilterSheet(
            filters = filters,
            categories = uiState.categories.orEmpty(),
            loadingCategories = uiState.loadingCategories,
            categoryLoadFailed = uiState.categoryLoadFailed,
            onSortChange = onSortChange,
            onCategoryChange = onCategoryChange,
            onTagsChange = onTagsChange,
            onRetryCategories = onRetryCategories,
            onApply = onApplyFilters,
            onDismiss = onCloseFilters,
        )
    }
}

@Composable
private fun SearchStatus(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(Spacing.m), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) { content() }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExploreScreenPreview() {
    AppTheme {
        ExploreScreen(
            uiState = ExploreUiState(),
            onQueryChange = {}, onSearch = {}, onSelectTab = {}, onUserClick = {}, onEventClick = {},
            onOpenFilters = {}, onCloseFilters = {}, onApplyFilters = {}, onSortChange = {},
            onCategoryChange = {}, onRetryCategories = {}, onBack = {},
        )
    }
}
