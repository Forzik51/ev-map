package com.evmap.mobileapp.feature.events

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.evmap.mobileapp.core.data.dto.EventDto
import com.evmap.mobileapp.core.data.dto.PageResponse
import com.evmap.mobileapp.feature.events.data.EventPagingSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class EventPagingSourceTest {
    @Test
    fun `refresh and append use the same page size even with a larger refresh request`() = runTest {
        val requests = mutableListOf<Pair<Int, Int>>()
        val source = EventPagingSource { page, size ->
            requests += page to size
            val events = (page * size until minOf(23, (page + 1) * size)).map { index ->
                EventDto(index.toLong(), "Event $index", "Description", "2030-03-22T18:30:00Z", userId = 1L)
            }
            PageResponse(events, page, size, 23, 2, page == 0, page == 1)
        }
        val first = source.load(PagingSource.LoadParams.Refresh(null, 60, false)) as PagingSource.LoadResult.Page
        val second = source.load(PagingSource.LoadParams.Append(first.nextKey!!, 20, false)) as PagingSource.LoadResult.Page

        assertEquals(listOf(0 to 20, 1 to 20), requests)
        assertEquals((0L..22L).toList(), (first.data + second.data).map { it.id })
        assertNull(second.nextKey)
    }

    @Test
    fun `feed skips overlapping IDs and refreshes from page zero after an append`() = runTest {
        val requests = mutableListOf<Pair<Int, Int>>()
        var failAppend = true
        val source = EventPagingSource(startFromFirstPage = true) { page, size ->
            requests += page to size
            if (page == 1 && failAppend) throw IOException("Append failed")
            val ids = if (page == 2) 20L..39L else 1L..20L
            val events = ids.map { id ->
                EventDto(id, "Event $id", "Description", "2030-03-22T18:30:00Z", userId = 1L)
            }
            PageResponse(events, page, size, 60, 3, page == 0, page == 2)
        }
        val first = source.load(PagingSource.LoadParams.Refresh(null, 20, false)) as PagingSource.LoadResult.Page
        val append = PagingSource.LoadParams.Append(first.nextKey!!, 20, false)
        assertTrue(source.load(append) is PagingSource.LoadResult.Error)
        failAppend = false
        val overlap = source.load(append) as PagingSource.LoadResult.Page
        assertTrue(overlap.data.isEmpty())
        val last = source.load(PagingSource.LoadParams.Append(overlap.nextKey!!, 20, false)) as PagingSource.LoadResult.Page
        val state = PagingState(listOf(first, overlap, last), 30, PagingConfig(pageSize = 20), 0)

        assertEquals(listOf(0 to 20, 1 to 20, 1 to 20, 2 to 20), requests)
        assertEquals((1L..39L).toList(), (first.data + last.data).map { it.id })
        assertNull(last.nextKey)
        assertNull(source.getRefreshKey(state))
    }

    @Test
    fun `empty feed ends pagination at the first page`() = runTest {
        val source = EventPagingSource(startFromFirstPage = true) { page, size ->
            PageResponse(emptyList(), page, size, 0, 0, first = true, last = true)
        }
        val result = source.load(PagingSource.LoadParams.Refresh(null, 20, false)) as PagingSource.LoadResult.Page

        assertTrue(result.data.isEmpty())
        assertNull(result.prevKey)
        assertNull(result.nextKey)
    }
}
