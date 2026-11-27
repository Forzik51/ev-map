package com.evmap.mobileapp.feature.reports.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evmap.mobileapp.core.data.dto.ReportCategoryDto
import com.evmap.mobileapp.feature.reports.data.ReportsRepository
import com.evmap.mobileapp.feature.reports.data.reportMessage
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
    val loadingCategories: Boolean = false,
    val categoryError: String? = null,
    val submitting: Boolean = false,
    val error: String? = null,
    val fieldErrors: Map<String,String> = emptyMap(),
    val submitted: Boolean = false,
)

@HiltViewModel
class ReportCreateViewModel @Inject constructor(
    private val repository: ReportsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ReportCreateUiState())
    val state: StateFlow<ReportCreateUiState> = _state

    init {
        loadCategories()
    }

    fun selectCategory(categoryId: Long) {
        _state.update { it.copy(selectedCategoryId = categoryId, error = null,fieldErrors=it.fieldErrors-"categoryId") }
    }

    fun updateDescription(value: String) {
        _state.update { it.copy(description = value, error = null,fieldErrors=it.fieldErrors-"description") }
    }

    fun submit(eventId: Long) {
        val current = state.value
        if (current.loadingCategories || current.submitting || current.submitted) return
        val categoryId = current.selectedCategoryId
        val description = current.description.trim()
        val errors=buildMap<String,String> {
            if(categoryId==null) put("categoryId","Виберіть категорію скарги")
            if(description.isBlank() || description.length>100) put("description","Опишіть проблему, до 100 символів")
        }
        val validationError = when {
            categoryId == null -> "Select report category"
            description.isBlank() -> "Describe the issue"
            description.length > 100 -> "Use up to 100 characters."
            else -> null
        }
        if (validationError != null) {
            _state.update { it.copy(error = validationError,fieldErrors=errors) }
            return
        }

        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            repository.create(eventId, requireNotNull(categoryId), description)
                .onSuccess {
                    _state.update { it.copy(submitting = false, submitted = true) }
                }
                .onFailure { error ->
                    _state.update { it.copy(submitting = false, error = error.reportMessage()) }
                }
        }
    }

    fun consumeSubmitted() {
        _state.update { it.copy(submitted = false) }
    }

    fun loadCategories() {
        val current = state.value
        if (current.loadingCategories || current.submitting) return
        _state.update { it.copy(loadingCategories = true, categoryError = null) }
        viewModelScope.launch {
            repository.categories()
                .onSuccess { categories ->
                    _state.update {
                        it.copy(
                            categories = categories,
                            selectedCategoryId = it.selectedCategoryId?.takeIf { id -> categories.any { category -> category.id == id } },
                            loadingCategories = false,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(loadingCategories = false, categoryError = error.reportMessage()) }
                }
        }
    }
}
