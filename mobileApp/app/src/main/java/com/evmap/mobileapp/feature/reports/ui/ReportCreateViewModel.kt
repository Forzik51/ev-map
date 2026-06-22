package com.evmap.mobileapp.feature.reports.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evmap.mobileapp.core.data.dto.ReportCategoryDto
import com.evmap.mobileapp.core.data.remote.toUserMessage
import com.evmap.mobileapp.feature.reports.data.ReportsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportCreateUiState(
    val categories: List<ReportCategoryDto> = emptyList(),
    val selectedCategoryId: Long? = null,
    val description: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val submitted: Boolean = false,
)

@HiltViewModel
class ReportCreateViewModel @Inject constructor(
    private val repository: ReportsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ReportCreateUiState(loading = true))
    val state: StateFlow<ReportCreateUiState> = _state

    init {
        loadCategories()
    }

    fun selectCategory(categoryId: Long) {
        _state.update { it.copy(selectedCategoryId = categoryId, error = null) }
    }

    fun updateDescription(value: String) {
        _state.update { it.copy(description = value, error = null) }
    }

    fun submit(eventId: Long) = viewModelScope.launch {
        val categoryId = _state.value.selectedCategoryId
        val description = _state.value.description.trim()
        val validationError = when {
            categoryId == null -> "Select report category"
            description.isBlank() -> "Describe the issue"
            else -> null
        }
        if (validationError != null) {
            _state.update { it.copy(error = validationError) }
            return@launch
        }

        _state.update { it.copy(loading = true, error = null) }
        repository.create(eventId, requireNotNull(categoryId), description)
            .onSuccess {
                _state.update { it.copy(loading = false, submitted = true) }
            }
            .onFailure { error ->
                _state.update { it.copy(loading = false, error = error.toUserMessage()) }
            }
    }

    fun consumeSubmitted() {
        _state.update { it.copy(submitted = false) }
    }

    private fun loadCategories() = viewModelScope.launch {
        repository.categories()
            .onSuccess { categories ->
                _state.value = ReportCreateUiState(categories = categories, loading = false)
            }
            .onFailure { error ->
                _state.value = ReportCreateUiState(loading = false, error = error.toUserMessage())
            }
    }
}
