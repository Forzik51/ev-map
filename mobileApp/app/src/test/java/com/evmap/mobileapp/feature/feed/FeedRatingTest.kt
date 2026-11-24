package com.evmap.mobileapp.feature.feed

import com.evmap.mobileapp.core.data.dto.EventDto
import com.evmap.mobileapp.feature.events.ui.displayRating
import com.evmap.mobileapp.feature.feed.data.toFeedItemUi
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class FeedRatingTest {
    @Test
    fun `feed distinguishes unrated events from zero and rounds the server average`() {
        val event = EventDto(1L, "Event", "Description", "2030-03-22T18:30:00Z", userId = 1L, commentsCount = 7)

        assertEquals("—", event.toFeedItemUi().event.rating.displayRating(Locale.US))
        assertEquals("0.0", event.copy(ratingAvg = 0.0).toFeedItemUi().event.rating.displayRating(Locale.US))
        val rated = event.copy(ratingAvg = 4.45).toFeedItemUi().event
        assertEquals("4.5", rated.rating.displayRating(Locale.US))
        assertEquals("4,5", rated.rating.displayRating(Locale.GERMANY))
        assertEquals(7, rated.reviewCount)
    }
}
