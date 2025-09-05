package com.evmap.mobileapp.feature.feed.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.evmap.mobileapp.feature.feed.data.EventsRepository
import com.evmap.mobileapp.feature.feed.data.toFeedItemUi
import core.ui.model.FeedItemUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    repo: EventsRepository,
    //private val api: EventsApi
) : ViewModel() {
    val feed: Flow<PagingData<FeedItemUi>> =
        repo.feed().map { paging -> paging.map { it.toFeedItemUi() } }.cachedIn(viewModelScope)

    //val feed = repo.feed()
    //    .map { paging -> paging.map { it.toFeedItemUi() } }
    //    .cachedIn(viewModelScope)

    //init {
    //    viewModelScope.launch {
    //        try {
    //            val res = api.getEvents(page = 0)
    //            android.util.Log.d("FeedVM", "ping OK, items=${res.content.size}")
    //        } catch (t: Throwable) {
    //            android.util.Log.e("FeedVM", "ping FAIL", t)
    //        }
    //    }
    //}
}