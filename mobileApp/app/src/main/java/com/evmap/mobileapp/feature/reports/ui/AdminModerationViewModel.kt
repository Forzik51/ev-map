package com.evmap.mobileapp.feature.reports.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evmap.mobileapp.core.data.dto.ReportDto
import com.evmap.mobileapp.core.data.dto.ReportStatus
import com.evmap.mobileapp.feature.reports.data.ReportsRepository
import com.evmap.mobileapp.feature.reports.data.reportMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

data class AdminModerationUiState(
    val reports: List<ReportDto> = emptyList(),
    val filter: ReportStatus = ReportStatus.OPEN,
    val loadError: String? = null,
    val accessDenied: Boolean = false,
    val subject: String = "",
    val body: String = "",
    val userId: String = "",
    val loading: Boolean = false,
    val sendingEmail: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

@HiltViewModel
class AdminModerationViewModel @Inject constructor(
    private val repository: ReportsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AdminModerationUiState(loading = true))
    val state: StateFlow<AdminModerationUiState> = _state

    private var loadJob: Job? = null

    fun selectFilter(status: ReportStatus) {
        if (status == state.value.filter) return
        _state.update { it.copy(filter = status, reports = emptyList()) }
        refresh()
    }

    fun refresh() {
        loadJob?.cancel()
        val filter = state.value.filter
        _state.update { it.copy(loading = true, loadError = null) }
        loadJob = viewModelScope.launch {
            repository.all(filter)
                .onSuccess { reports ->
                    _state.update { it.copy(reports = reports, loading = false, accessDenied = false) }
                }
                .onFailure { error ->
                    val denied = (error as? HttpException)?.code() in listOf(401, 403)
                    _state.update {
                        it.copy(
                            reports = if (denied) emptyList() else it.reports,
                            loading = false,
                            accessDenied = denied || it.accessDenied,
                            loadError = error.reportMessage(),
                        )
                    }
                }
        }
    }

    fun updateSubject(value: String) = _state.update { it.copy(subject = value, message = null, error = null) }

    fun updateBody(value: String) = _state.update { it.copy(body = value, message = null, error = null) }

    fun updateUserId(value: String) = _state.update { it.copy(userId = value, message = null, error = null) }

    fun sendToAll() = sendEmail { subject, body ->
        repository.sendEmailToAll(subject, body).map { summary ->
            when {
                summary.sent == 0 && summary.failed == 0 -> "No recipients"
                summary.failed == 0 -> "Submitted to SMTP: ${summary.sent}"
                else -> "Submitted to SMTP: ${summary.sent}. Failed: ${summary.failed}."
            }
        }
    }

    fun sendToUser() {
        val userId = _state.value.userId.toLongOrNull()
        if (userId == null) {
            _state.update { it.copy(error = "Enter valid user ID") }
            return
        }
        sendEmail { subject, body -> repository.sendEmailToUser(userId, subject, body).map { "Email sent" } }
    }

    private fun sendEmail(request: suspend (String, String) -> Result<String>) = viewModelScope.launch {
        if (_state.value.sendingEmail) return@launch
        val subject = _state.value.subject.trim()
        val body = _state.value.body.trim()
        if (subject.isBlank() || body.isBlank()) {
            _state.update { it.copy(error = "Subject and body are required") }
            return@launch
        }

        _state.update { it.copy(sendingEmail = true, message = null, error = null) }
        request(subject, body)
            .onSuccess { message -> _state.update { it.copy(message = message, error = null, sendingEmail = false) } }
            .onFailure { error -> _state.update { it.copy(error = error.reportMessage(), message = null, sendingEmail = false) } }
    }
}
