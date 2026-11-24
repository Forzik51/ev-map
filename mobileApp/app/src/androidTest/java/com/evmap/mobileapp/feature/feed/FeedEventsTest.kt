package com.evmap.mobileapp.feature.feed

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.map
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.evmap.mobileapp.core.data.dto.EventDto
import com.evmap.mobileapp.core.data.dto.PageResponse
import com.evmap.mobileapp.feature.events.data.EventPagingSource
import com.evmap.mobileapp.feature.events.ui.displayRating
import com.evmap.mobileapp.feature.feed.data.toFeedItemUi
import com.evmap.mobileapp.feature.feed.ui.FeedEvents
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class FeedEventsTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun firstLoadErrorCanBeRetriedToAnEmptyFeed() {
        val attempts = AtomicInteger()
        val pager = Pager(PagingConfig(pageSize = 20, initialLoadSize = 20)) {
            EventPagingSource(startFromFirstPage = true) { page, size ->
                if (attempts.getAndIncrement() == 0) throw IOException("Offline")
                PageResponse(emptyList(), page, size, 0, 0, first = true, last = true)
            }
        }

        showFeed(pager.flow)
        awaitText("Check your connection and try again.")
        compose.onNodeWithText("Retry").performClick()
        awaitText("No events yet")
    }

    @Test
    fun appendAndRefreshErrorsKeepCardsAndCanBeRetried() {
        val firstPageLoads = AtomicInteger()
        val appendAttempts = AtomicInteger()
        val pager = Pager(PagingConfig(pageSize = 20, initialLoadSize = 20, prefetchDistance = 1)) {
            EventPagingSource(startFromFirstPage = true) { page, size ->
                if (page == 1 && appendAttempts.getAndIncrement() == 0) throw IOException("Offline")
                val rating = if (page == 0) {
                    when (firstPageLoads.incrementAndGet()) {
                        1 -> null
                        2 -> throw IOException("Offline")
                        else -> 4.45
                    }
                } else {
                    null
                }
                val id = page.toLong() + 1
                val event = EventDto(id, "Event $id", "Description", "2030-03-22T18:30:00Z", userId = 1L, ratingAvg = rating)
                PageResponse(listOf(event), page, size, 2, 2, first = page == 0, last = page == 1)
            }
        }

        showFeed(pager.flow)
        awaitText("Event 1")
        val list = compose.onNode(hasScrollToIndexAction())
        compose.waitUntil(timeoutMillis = 5_000) { appendAttempts.get() > 0 }
        list.performScrollToIndex(1)
        awaitText("Check your connection and try again.")
        list.performScrollToIndex(0)
        compose.onNodeWithText("Event 1").assertIsDisplayed()
        list.performScrollToIndex(1)
        compose.onNodeWithText("Retry").performClick()
        awaitText("Event 2")

        list.performScrollToIndex(0)
        list.performTouchInput { swipeDown(startY = height * 0.2f, endY = height * 0.85f) }
        awaitText("Check your connection and try again.")
        compose.onNodeWithText("Event 1").assertIsDisplayed()
        compose.onNodeWithText("Retry").performClick()
        awaitText(4.45.displayRating())
    }

    private fun showFeed(feed: Flow<PagingData<EventDto>>) {
        val items = feed.map { page -> page.map(EventDto::toFeedItemUi) }
        compose.setContent {
            MaterialTheme {
                FeedEvents(items.collectAsLazyPagingItems(), onOpenItem = {})
            }
        }
    }

    private fun awaitText(text: String) {
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
