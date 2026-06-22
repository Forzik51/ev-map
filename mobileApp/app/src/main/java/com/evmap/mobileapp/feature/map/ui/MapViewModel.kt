package com.evmap.mobileapp.feature.map.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evmap.mobileapp.core.data.dto.MapEventItemDto
import com.evmap.mobileapp.core.data.remote.toUserMessage
import com.evmap.mobileapp.feature.map.data.MapRepository
import com.evmap.mobileapp.feature.map.data.toEventUi
import com.evmap.mobileapp.feature.map.data.toMapPinUi
import core.ui.model.EventUi
import core.ui.model.MapCameraUi
import core.ui.model.MapPinUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val query: String = "",
    val pins: List<MapPinUi> = emptyList(),
    val camera: MapCameraUi = MapCameraUi(lat = WARSAW_LAT, lng = WARSAW_LNG, zoom = 11f),
    val selected: EventUi? = null,
    val loading: Boolean = false,
    val error: String? = null,
) {
    companion object {
        const val WARSAW_LAT = 52.2297
        const val WARSAW_LNG = 21.0122
    }
}

@HiltViewModel
class MapViewModel @Inject constructor(
    private val repository: MapRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(MapUiState())
    val state: StateFlow<MapUiState> = _state

    private var items: List<MapEventItemDto> = emptyList()

    init {
        loadNearby()
    }

    fun updateQuery(value: String) {
        _state.update { it.copy(query = value) }
        if (value.isBlank()) {
            loadNearby()
        } else if (value.length >= MIN_SEARCH_LENGTH) {
            search(value)
        }
    }

    fun selectPin(pinId: String) {
        _state.update { state ->
            state.copy(selected = items.firstOrNull { it.eventId.toString() == pinId }?.toEventUi())
        }
    }

    fun dismissPreview() {
        _state.update { it.copy(selected = null) }
    }

    fun zoomIn() {
        _state.update { it.copy(camera = it.camera.copy(zoom = it.camera.zoom + 1f)) }
    }

    fun zoomOut() {
        _state.update { it.copy(camera = it.camera.copy(zoom = (it.camera.zoom - 1f).coerceAtLeast(1f))) }
    }

    fun loadNearby() = viewModelScope.launch {
        val camera = _state.value.camera
        _state.update { it.copy(loading = true, error = null) }
        repository.nearby(camera.lat, camera.lng)
            .onSuccess(::setItems)
            .onFailure { error ->
                _state.update { it.copy(loading = false, error = error.toUserMessage()) }
            }
    }

    private fun search(query: String) = viewModelScope.launch {
        val camera = _state.value.camera
        _state.update { it.copy(loading = true, error = null) }
        repository.search(query, camera.lat, camera.lng)
            .onSuccess(::setItems)
            .onFailure { error ->
                _state.update { it.copy(loading = false, error = error.toUserMessage()) }
            }
    }

    private fun setItems(nextItems: List<MapEventItemDto>) {
        items = nextItems
        _state.update {
            it.copy(
                pins = nextItems.map { item -> item.toMapPinUi() },
                selected = null,
                loading = false,
            )
        }
    }

    private companion object {
        const val MIN_SEARCH_LENGTH = 2
    }
}
