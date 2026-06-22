package com.evmap.mobileapp.feature.reports.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evmap.mobileapp.core.data.dto.ReportCategoryDto
import com.evmap.mobileapp.core.data.dto.ReportDto
import com.evmap.mobileapp.core.data.remote.toUserMessage
import com.evmap.mobileapp.feature.reports.data.ReportsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminModerationUiState(
    val reports: List<ReportDto> = emptyList(),
    val categories: List<ReportCategoryDto> = emptyList(),
    val subject: String = "",
    val body: String = "",
    val userId: String = "",
    val loading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

@HiltViewModel
class AdminModerationViewModel @Inject constructor(
    private val repository: ReportsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AdminModerationUiState(loading = true))
    val state: StateFlow<AdminModerationUiState> = _state

    init {
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        val categories = repository.categories().getOrDefault(emptyList())
        repository.all()
            .onSuccess { reports ->
                _state.update {
                    it.copy(reports = reports, categories = categories, loading = false)
                }
            }
            .onFailure { error ->
                _state.update { it.copy(loading = false, error = error.toUserMessage()) }
            }
    }

    fun updateStatus(reportId: Long, status: String) = viewModelScope.launch {
        repository.updateStatus(reportId, status)
            .onSuccess {
                _state.update { state ->
                    state.copy(reports = state.reports.map { if (it.id == reportId) it.copy(status = status) else it })
                }
            }
            .onFailure { error -> _state.update { it.copy(error = error.toUserMessage()) } }
    }

    fun delete(reportId: Long) = viewModelScope.launch {
        repository.delete(reportId)
            .onSuccess {
                _state.update { state -> state.copy(reports = state.reports.filterNot { it.id == reportId }) }
            }
            .onFailure { error -> _state.update { it.copy(error = error.toUserMessage()) } }
    }

    fun updateSubject(value: String) = _state.update { it.copy(subject = value, message = null, error = null) }

    fun updateBody(value: String) = _state.update { it.copy(body = value, message = null, error = null) }

    fun updateUserId(value: String) = _state.update { it.copy(userId = value, message = null, error = null) }

    fun sendToAll() = sendEmail { subject, body ->
        repository.sendEmailToAll(subject, body)
    }

    fun sendToUser() {
        val userId = _state.value.userId.toLongOrNull()
        if (userId == null) {
            _state.update { it.copy(error = "Enter valid user ID") }
            return
        }
        sendEmail { subject, body -> repository.sendEmailToUser(userId, subject, body) }
    }

    private fun sendEmail(request: suspend (String, String) -> Result<Unit>) = viewModelScope.launch {
        val subject = _state.value.subject.trim()
        val body = _state.value.body.trim()
        if (subject.isBlank() || body.isBlank()) {
            _state.update { it.copy(error = "Subject and body are required") }
            return@launch
        }

        request(subject, body)
            .onSuccess { _state.update { it.copy(message = "Email sent", error = null) } }
            .onFailure { error -> _state.update { it.copy(error = error.toUserMessage(), message = null) } }
    }
}
