package com.evmap.mobileapp.feature.map

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.evmap.mobileapp.core.data.dto.MapClusterDto
import com.evmap.mobileapp.core.data.dto.MapEventItemDto
import com.evmap.mobileapp.core.data.dto.MapPointDto
import com.evmap.mobileapp.core.data.remote.MapApi
import com.evmap.mobileapp.feature.map.data.MapRepository
import com.evmap.mobileapp.feature.map.ui.MapViewModel
import core.ui.model.MapCameraUi
import core.ui.model.MapViewport
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val api = FakeMapApi()
    private lateinit var model: MapViewModel

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        model = MapViewModel(SavedStateHandle(), MapRepository(api))
    }

    @After fun cleanup() {
        model.viewModelScope.cancel()
        Dispatchers.resetMain()
    }

    @Test fun splitsDatelineBoundsAndDeduplicatesEventIds() = runTest(dispatcher) {
        val visible = MapViewport(10.0, 175.0, 20.0, -175.0, 12)
        model.cameraStopped(camera(12), visible)
        runCurrent()

        assertEquals(listOf(175.0 to 180.0, -180.0 to -175.0), api.bounds)
        assertEquals(listOf("1"), model.state.value.pins.map { it.id })
        assertEquals(visible.apiBounds(), visible.copy(east = 185.0).apiBounds())
        assertEquals(listOf(-180.0 to 180.0), visible.copy(west = -185.0, east = 185.0).apiBounds().map { it.west to it.east })
    }

    @Test fun crossesZoomThresholdAndIgnoresOutdatedViewportResponse() = runTest(dispatcher) {
        val lateClusters = CompletableDeferred<List<MapClusterDto>>()
        api.loadClusters = { withContext(NonCancellable) { lateClusters.await() } }
        val visible = MapViewport(51.0, 20.0, 53.0, 22.0, 11)
        model.cameraStopped(camera(11), visible)
        runCurrent()
        assertEquals(listOf(11), api.clusterZooms)

        model.cameraStopped(camera(12), visible.copy(zoom = 12))
        runCurrent()
        assertEquals("1", model.state.value.pins.single().id)
        lateClusters.complete(listOf(MapClusterDto(20, MapPointDto(52.0, 21.0))))
        runCurrent()
        assertTrue(model.state.value.clusters.isEmpty())
        assertEquals("1", model.state.value.pins.single().id)
    }

    @Test fun debouncesSearchAndClearingItLoadsVisibleBoundsAgain() = runTest(dispatcher) {
        val visible = MapViewport(51.0, 20.0, 53.0, 22.0, 12)
        model.cameraStopped(camera(12), visible)
        runCurrent()

        model.updateQuery("Mu")
        advanceTimeBy(150)
        model.updateQuery("Music")
        advanceTimeBy(299)
        assertTrue(api.searches.isEmpty())
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf("Music"), api.searches)
        model.cameraStopped(camera(13), visible.copy(zoom = 13, south = 51.5))
        runCurrent()
        assertEquals(1, api.bounds.size)

        model.updateQuery("")
        runCurrent()
        assertEquals(2, api.bounds.size)
    }

    private fun camera(zoom: Int) = MapCameraUi(52.0, 21.0, zoom.toFloat())

    private class FakeMapApi : MapApi {
        val bounds = mutableListOf<Pair<Double, Double>>()
        val clusterZooms = mutableListOf<Int>()
        val searches = mutableListOf<String>()
        var loadClusters: suspend () -> List<MapClusterDto> = { emptyList() }

        override suspend fun nearby(lat: Double, lng: Double, radiusMeters: Double?, limit: Int?) = error("Unexpected nearby request")

        override suspend fun bounds(minLat: Double, minLng: Double, maxLat: Double, maxLng: Double, limit: Int?): List<MapEventItemDto> {
            bounds += minLng to maxLng
            return listOf(event("Viewport event"))
        }

        override suspend fun clusters(minLat: Double, minLng: Double, maxLat: Double, maxLng: Double, zoom: Int): List<MapClusterDto> {
            clusterZooms += zoom
            return loadClusters()
        }

        override suspend fun details(eventId: Long) = error("Unexpected detail request")

        override suspend fun search(query: String, lat: Double?, lng: Double?, limit: Int?): List<MapEventItemDto> {
            searches += query
            return listOf(event(query))
        }

        private fun event(name: String) = MapEventItemDto(
            1, name, null, "POINT", MapPointDto(52.0, 21.0), null, null, 0.0, 0, emptyList(),
        )
    }
}
