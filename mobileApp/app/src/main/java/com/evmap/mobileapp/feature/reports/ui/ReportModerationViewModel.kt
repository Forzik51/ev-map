package com.evmap.mobileapp.feature.reports.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evmap.mobileapp.core.data.dto.ReportDetailsDto
import com.evmap.mobileapp.core.data.dto.ReportStatus
import com.evmap.mobileapp.feature.reports.data.ReportsRepository
import com.evmap.mobileapp.feature.reports.data.reportMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

internal const val MAX_DECISION_REASON_LENGTH = 500

data class ReportModerationUiState(
    val details: ReportDetailsDto? = null,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val deleting: Boolean = false,
    val deleted: Boolean = false,
    val outcome: ReportStatus? = null,
    val reason: String = "",
    val loadError: String? = null,
    val actionError: String? = null,
    val message: String? = null,
) {
    val canDecide: Boolean
        get() = details?.report?.status == ReportStatus.OPEN && loadError == null && !loading && !saving && !deleting && !deleted

    val canDelete: Boolean
        get() = canDecide && details?.decisions?.isEmpty() == true
}

@HiltViewModel
class ReportModerationViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: ReportsRepository,
) : ViewModel() {
    private val reportId = checkNotNull(savedState.get<Long>("reportId"))
    private val _state = MutableStateFlow(ReportModerationUiState())
    val state = _state.asStateFlow()
    private var loadJob: Job? = null

    fun refresh() {
        val current = state.value
        if (current.saving || current.deleting || current.deleted || loadJob?.isActive == true) return
        _state.update { it.copy(loading = true, loadError = null) }
        loadJob = viewModelScope.launch {
            repository.details(reportId)
                .onSuccess { details ->
                    val open = details.report.status == ReportStatus.OPEN
                    _state.update {
                        it.copy(
                            details = details,
                            loading = false,
                            outcome = it.outcome.takeIf { open },
                            reason = if (open) it.reason else "",
                            actionError = it.actionError.takeIf { open },
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            details = it.details.takeUnless { error.reportUnavailable() },
                            loading = false,
                            loadError = error.reportMessage(),
                        )
                    }
                }
        }
    }

    fun chooseOutcome(outcome: ReportStatus) {
        if (!state.value.canDecide || outcome == ReportStatus.OPEN) return
        _state.update { it.copy(outcome = outcome, reason = "", actionError = null, message = null) }
    }

    fun editReason(reason: String) {
        if (state.value.saving) return
        _state.update { it.copy(reason = reason, actionError = null) }
    }

    fun cancelDecision() {
        if (state.value.saving) return
        _state.update { it.copy(outcome = null, reason = "", actionError = null) }
    }

    fun submitDecision() {
        val current = state.value
        val outcome = current.outcome ?: return
        val reason = current.reason.trim()
        if (!current.canDecide || reason.length !in 1..MAX_DECISION_REASON_LENGTH) return
        _state.update { it.copy(saving = true, actionError = null, message = null) }
        viewModelScope.launch {
            repository.decide(reportId, outcome, reason)
                .onSuccess { details ->
                    _state.update {
                        it.copy(details = details, saving = false, outcome = null, reason = "", message = "Decision saved.")
                    }
                }
                .onFailure { handleActionError(it, "This report has already been reviewed.") }
        }
    }

    fun delete() {
        if (!state.value.canDelete) return
        _state.update { it.copy(deleting = true, actionError = null, message = null) }
        viewModelScope.launch {
            repository.delete(reportId)
                .onSuccess { _state.update { it.copy(deleting = false, deleted = true) } }
                .onFailure { handleActionError(it, "This report can no longer be deleted.") }
        }
    }

    private fun handleActionError(error: Throwable, conflictMessage: String) {
        val conflict = (error as? HttpException)?.code() == 409
        val unavailable = error.reportUnavailable()
        _state.update {
            it.copy(
                details = it.details.takeUnless { unavailable },
                saving = false,
                deleting = false,
                actionError = error.reportMessage().takeUnless { conflict || unavailable },
                loadError = if (unavailable) error.reportMessage() else it.loadError,
                message = conflictMessage.takeIf { conflict },
            )
        }
        if (conflict) refresh()
    }

    private fun Throwable.reportUnavailable() = (this as? HttpException)?.code() in listOf(401, 403, 404)
}
