package com.evmap.mobileapp.feature.events

import com.evmap.mobileapp.feature.events.ui.EventFormState
import com.evmap.mobileapp.feature.events.ui.toRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant

class EventFormStateTest {
    private val startsAt = Instant.parse("2030-03-22T18:30:00Z")
    private val endsAt = Instant.parse("2030-03-23T00:15:00Z")
    private val existingEvent = EventFormState(
        editing = true,
        title = "Original title",
        description = "Description",
        startsAt = startsAt,
        endsAt = endsAt,
        locationId = 12L,
        categoryIds = setOf(4L, 8L),
    )

    @Test
    fun `title only edit keeps exact start time end time location and categories`() {
        val request = existingEvent.copy(title = " Edited title ").toRequest()
        assertEquals("Edited title", request.name)
        assertEquals(startsAt.toString(), request.startsAt)
        assertEquals(endsAt.toString(), request.endsAt)
        assertEquals(12L, request.locationId)
        assertEquals(setOf(4L, 8L), request.categoryIds.toSet())
    }

    @Test
    fun `invalid event date range is rejected before submitting`() {
        assertThrows(IllegalArgumentException::class.java) {
            existingEvent.copy(endsAt = startsAt.minusSeconds(1)).toRequest()
        }
    }
}
