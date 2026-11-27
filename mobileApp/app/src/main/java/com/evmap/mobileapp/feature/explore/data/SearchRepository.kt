package com.evmap.mobileapp.feature.explore.data

import com.evmap.mobileapp.core.data.dto.SearchResponseDto
import com.evmap.mobileapp.core.data.remote.SearchApi
import feature.explore.ExploreFilters
import feature.explore.ExploreTab
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepository @Inject constructor(
    private val searchApi: SearchApi,
) {
    suspend fun search(query: String, tab: ExploreTab, filters: ExploreFilters): Result<SearchResponseDto> = try {
        val eventFilters = filters.takeIf { tab == ExploreTab.EVENTS }
        Result.success(
            searchApi.search(
                query = query,
                type = tab.apiValue,
                limit = 30,
                sort = eventFilters?.sort?.apiValue,
                categoryIds = eventFilters?.categoryId?.let(::listOf),
                tags = eventFilters?.tagsText?.split(',')?.map(String::trim)?.filter(String::isNotEmpty),
            )
        )
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(error)
    }
}
