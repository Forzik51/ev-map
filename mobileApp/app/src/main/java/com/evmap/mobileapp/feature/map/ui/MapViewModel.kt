package com.evmap.mobileapp.feature.map.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evmap.mobileapp.core.data.dto.MapClusterDto
import com.evmap.mobileapp.core.data.remote.toUserMessage
import com.evmap.mobileapp.feature.map.data.DeviceLocation
import com.evmap.mobileapp.feature.map.data.MapRepository
import com.evmap.mobileapp.feature.map.data.toEventUi
import com.evmap.mobileapp.feature.map.data.toMapPinUi
import core.ui.model.EventUi
import core.ui.model.MapCameraUi
import core.ui.model.MapPinUi
import core.ui.model.MapViewport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal val initialMapCamera = MapCameraUi(52.2297, 21.0122, 11f)

sealed interface MapCameraCommand {
    data class Center(val camera: MapCameraUi) : MapCameraCommand
    data class Zoom(val delta: Double) : MapCameraCommand
    data class Fit(val pins: List<MapPinUi>) : MapCameraCommand
}

data class MapUiState(
    val query: String = "",
    val pins: List<MapPinUi> = emptyList(),
    val clusters: List<MapClusterDto> = emptyList(),
    val location: DeviceLocation? = null,
    val selected: EventUi? = null,
    val selectedPin: MapPinUi? = null,
    val pinChoices: List<MapPinUi> = emptyList(),
    val detailsLoading: Boolean = false,
    val detailsError: String? = null,
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val repository: MapRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(MapUiState())
    val state = _state.asStateFlow()

    private var camera = MapCameraUi(
        lat = savedState.get<Double>("mapLatitude") ?: initialMapCamera.lat,
        lng = savedState.get<Double>("mapLongitude") ?: initialMapCamera.lng,
        zoom = savedState.get<Float>("mapZoom") ?: initialMapCamera.zoom,
    )
    fun cameraPosition(): MapCameraUi = camera

    private var viewport: MapViewport? = null
    private var loadJob: Job? = null
    private var detailsJob: Job? = null
    private val cameraCommands = Channel<MapCameraCommand>(Channel.BUFFERED)
    val commands = cameraCommands.receiveAsFlow()

    fun cameraStopped(position: MapCameraUi, visible: MapViewport) {
        camera = position
        savedState["mapLatitude"] = position.lat
        savedState["mapLongitude"] = position.lng
        savedState["mapZoom"] = position.zoom
        if (viewport == visible) return
        val changedMode = viewport?.showClusters != visible.showClusters
        viewport = visible
        if (state.value.query.isBlank()) {
            if (changedMode) _state.update { it.copy(pins = emptyList(), clusters = emptyList()) }
            loadViewport()
        }
    }

    fun updateQuery(value: String) {
        val query = value.trim()
        val changed = query != state.value.query.trim()
        _state.update { it.copy(query = value) }
        if (!changed) return
        dismissPreview()
        loadJob?.cancel()
        _state.update {
            it.copy(pins = emptyList(), clusters = emptyList(), loading = false, loaded = false, error = null)
        }
        when {
            query.isEmpty() -> loadViewport()
            query.length >= 2 -> search(query, debounce = true)
        }
    }

    fun retry() {
        val query = state.value.query.trim()
        if (query.isEmpty()) loadViewport()
        else if (query.length >= 2) search(query, debounce = false)
    }

    private fun loadViewport() {
        val visible = viewport ?: return
        loadJob?.cancel()
        _state.update { it.copy(loading = true, error = null) }
        loadJob = viewModelScope.launch {
            if (visible.showClusters) {
                val result = repository.clusters(visible)
                ensureActive()
                result.onSuccess { clusters ->
                    _state.update { it.copy(clusters = clusters, pins = emptyList(), loading = false, loaded = true) }
                }.onFailure(::showLoadError)
            } else {
                val result = repository.bounds(visible)
                ensureActive()
                result.onSuccess { items ->
                    _state.update { it.copy(pins = items.map { item -> item.toMapPinUi() }, clusters = emptyList(), loading = false, loaded = true) }
                }.onFailure(::showLoadError)
            }
        }
    }

    private fun search(query: String, debounce: Boolean) {
        loadJob?.cancel()
        _state.update { it.copy(loading = true, error = null) }
        loadJob = viewModelScope.launch {
            if (debounce) delay(300)
            val result = repository.search(query, camera.lat, camera.lng)
            ensureActive()
            result.onSuccess { items ->
                val pins = items.map { it.toMapPinUi() }
                _state.update { it.copy(pins = pins, clusters = emptyList(), loading = false, loaded = true) }
                if (pins.isNotEmpty()) cameraCommands.trySend(MapCameraCommand.Fit(pins))
            }.onFailure(::showLoadError)
        }
    }

    private fun showLoadError(error: Throwable) {
        _state.update { it.copy(loading = false, error = error.toUserMessage()) }
    }

    fun selectPins(ids: Set<String>) {
        val pins = state.value.pins
        val touched = pins.filter { it.id in ids }
        val points = touched.mapTo(hashSetOf()) { it.lat to it.lng }
        val matches = pins.filter { (it.lat to it.lng) in points }
        if (matches.size == 1) selectPin(matches.single())
        else if (matches.isNotEmpty()) {
            dismissPreview()
            _state.update { it.copy(pinChoices = matches) }
        }
    }

    fun selectPin(pin: MapPinUi) {
        val eventId = pin.id.toLongOrNull() ?: return
        detailsJob?.cancel()
        _state.update {
            it.copy(selectedPin = pin, pinChoices = emptyList(), selected = null, detailsLoading = true, detailsError = null)
        }
        detailsJob = viewModelScope.launch {
            val result = repository.details(eventId)
            ensureActive()
            result.onSuccess { details ->
                _state.update { it.copy(selected = details.toEventUi(), detailsLoading = false) }
            }.onFailure { error ->
                _state.update { it.copy(detailsLoading = false, detailsError = error.toUserMessage()) }
            }
        }
    }

    fun dismissPreview() {
        detailsJob?.cancel()
        _state.update {
            it.copy(selected = null, selectedPin = null, pinChoices = emptyList(), detailsLoading = false, detailsError = null)
        }
    }

    fun zoom(delta: Double) { cameraCommands.trySend(MapCameraCommand.Zoom(delta)) }

    fun recenter() { cameraCommands.trySend(MapCameraCommand.Center(initialMapCamera)) }

    fun selectCluster(lat: Double, lng: Double) {
        cameraCommands.trySend(MapCameraCommand.Center(MapCameraUi(lat, lng, (camera.zoom + 2f).coerceAtMost(22f))))
    }

    fun showLocation(location: DeviceLocation) {
        _state.update { it.copy(location = location) }
        val zoom = if (location.precise) 15f else 12f
        cameraCommands.trySend(MapCameraCommand.Center(MapCameraUi(location.lat, location.lng, zoom)))
    }

    fun clearLocation() { _state.update { it.copy(location = null) } }
}
