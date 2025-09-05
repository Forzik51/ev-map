package feature.feed.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import core.designsystem.Spacing
import core.ui.components.AppBottomBar
import core.ui.components.SearchField
import core.ui.model.EventUi
import core.ui.model.FeedItemUi
import feature.events.ui.components.EventCard


@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun FeedScreenPreview() {
    val e1 = EventUi(
        id = "event1",
        title = "First",
        location = "Hello",
        startsAt = "000000",
        description = "Preview item",
        imageUrl = null,
        rating = 4.6F,
        reviewCount = 120
    )
    val e2 = EventUi(
        id = "event2",
        title = "Second",
        location = "World",
        startsAt = "000000",
        description = "Preview item",
        imageUrl = null,
        rating = 4.8F,
        reviewCount = 87
    )

    val sampleItems = listOf(
        FeedItemUi(id = "1", event = e1, timestamp = System.currentTimeMillis(), isPromoted = true),
        FeedItemUi(id = "2", event = e2)
    )

    FeedScreen(
        query = "",
        items = sampleItems,
        currentRoute = "feed",
        onQueryChange = {},
        onOpenItem = {},
        onOpenSearch = {},
        onOpenCreate = {},
        onNavigate = {},
        modifier = Modifier
    )
}



@Composable
fun FeedScreen(
    query: String,
    items: List<FeedItemUi>,
    currentRoute: String?,
    onQueryChange: (String) -> Unit,
    onOpenItem: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenCreate: () -> Unit,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            AppBottomBar(
                currentRoute = currentRoute,
                onNavigate = onNavigate
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(paddingValues)
        ) {
            // Search field
            SearchField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = "Hinted search text",
                onSearch = onOpenSearch,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.m)
                    .padding(top = Spacing.m)
            )
            
            Spacer(modifier = Modifier.height(Spacing.m))
            
            // Feed content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = Spacing.m),
                verticalArrangement = Arrangement.spacedBy(Spacing.m)
            ) {
                items(
                    items = items,
                    key = { it.id }
                ) { feedItem ->
                    EventCard(
                        event = feedItem.event,
                        onClick = { onOpenItem(feedItem.event.id) }
                    )
                }
            }
        }
    }
}
