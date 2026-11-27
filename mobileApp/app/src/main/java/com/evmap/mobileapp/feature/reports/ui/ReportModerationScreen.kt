package com.evmap.mobileapp.feature.reports.ui
import androidx.compose.ui.res.stringResource
import com.evmap.mobileapp.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.evmap.mobileapp.core.data.dto.ReportDecisionDto
import com.evmap.mobileapp.core.data.dto.ReportStatus
import core.designsystem.Spacing
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReportModerationScreen(
    state: ReportModerationUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenEvent: (Long) -> Unit,
    onChooseOutcome: (ReportStatus) -> Unit,
    onEditReason: (String) -> Unit,
    onCancelDecision: () -> Unit,
    onSubmitDecision: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val details = state.details
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.form_review_report_29a1c5)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.form_back_b52b36))
                    }
                },
                actions = {
                    TextButton(onClick = onRefresh, enabled = !state.loading && !state.saving && !state.deleting) {
                        Text(stringResource(R.string.form_refresh_56e3ba))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            if (state.loading || state.deleting) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.loadError?.let { error ->
                item {
                    Text(error, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRefresh) { Text(stringResource(R.string.form_retry_9f5cd8)) }
                }
            }
            state.message?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.primary) } }
            if (state.outcome == null) state.actionError?.let { error ->
                item { Text(error, color = MaterialTheme.colorScheme.error) }
            }
            if (details != null) {
                val report = details.report
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Text(report.eventName, style = MaterialTheme.typography.titleLarge)
                        Text("${report.categoryName} · ${report.status.label}")
                        Text("Report #${report.id} · Submitted by user #${report.userId}", style = MaterialTheme.typography.bodySmall)
                        Text(report.description)
                        OutlinedButton(
                            onClick = { onOpenEvent(report.eventId) },
                            enabled = !state.saving && !state.deleting,
                        ) { Text(stringResource(R.string.form_open_event_62871b)) }
                    }
                }
                if (report.status == ReportStatus.OPEN) {
                    item {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                            Button(
                                onClick = { onChooseOutcome(ReportStatus.RESOLVED) },
                                enabled = state.canDecide,
                            ) { Text(stringResource(R.string.form_confirm_report_4cc623)) }
                            OutlinedButton(
                                onClick = { onChooseOutcome(ReportStatus.DISMISSED) },
                                enabled = state.canDecide,
                            ) { Text(stringResource(R.string.form_dismiss_report_004232)) }
                        }
                    }
                }
                if (details.decisions.isNotEmpty()) {
                    item { Text(stringResource(R.string.form_decisions_af2f32), style = MaterialTheme.typography.titleMedium) }
                    items(details.decisions, key = { it.id }) { decision -> DecisionCard(decision) }
                } else if (report.status != ReportStatus.OPEN) {
                    item { Text(stringResource(R.string.form_no_decision_details_were_recorded_91726d)) }
                }
                if (state.canDelete) {
                    item {
                        TextButton(onClick = { confirmDelete = true }) {
                            Text(stringResource(R.string.form_delete_report_c669ce), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    val outcome = state.outcome
    if (outcome != null && details?.report?.status == ReportStatus.OPEN) {
        val reasonLength = state.reason.trim().length
        AlertDialog(
            onDismissRequest = onCancelDecision,
            title = { Text(if (outcome == ReportStatus.RESOLVED) "Confirm report?" else "Dismiss report?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    OutlinedTextField(
                        value = state.reason,
                        onValueChange = onEditReason,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.saving,
                        minLines = 3,
                        maxLines = 5,
                        label = { Text(stringResource(R.string.form_reason_f219cc)) },
                        supportingText = { Text("$reasonLength/$MAX_DECISION_REASON_LENGTH") },
                        isError = reasonLength > MAX_DECISION_REASON_LENGTH,
                    )
                    state.actionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                    state.loadError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onRefresh) { Text(stringResource(R.string.form_retry_9f5cd8)) }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onSubmitDecision,
                    enabled = state.canDecide && reasonLength in 1..MAX_DECISION_REASON_LENGTH,
                ) { Text(if (state.saving) "Saving…" else "Save decision") }
            },
            dismissButton = {
                TextButton(onClick = onCancelDecision, enabled = !state.saving) { Text(stringResource(R.string.form_cancel_77dfd2)) }
            },
        )
    }
    if (confirmDelete && state.canDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.form_delete_report_b29b7d)) },
            text = { Text(stringResource(R.string.form_the_report_will_be_removed_from_the_mo_681911)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text(stringResource(R.string.form_delete_report_c669ce), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.form_cancel_77dfd2)) } },
        )
    }
}

@Composable
private fun DecisionCard(decision: ReportDecisionDto) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Text(decision.outcome?.label ?: "Previous decision", style = MaterialTheme.typography.titleSmall)
            Text(decision.reason)
            val reviewer = decision.moderatorUsername ?: decision.moderatorId?.let { "User #$it" } ?: "Reviewer unavailable"
            val recorded = decision.decidedAt?.let { "$reviewer · ${formatDecisionTime(it)}" } ?: reviewer
            Text(recorded, style = MaterialTheme.typography.bodySmall)
            decision.blockedUntil?.let { Text("Recorded block until ${formatDecisionTime(it)}") }
        }
    }
}

private fun formatDecisionTime(value: String): String =
    runCatching {
        OffsetDateTime.parse(value)
            .atZoneSameInstant(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))
    }.getOrDefault(value)

internal val ReportStatus.label: String
    get() = when (this) {
        ReportStatus.OPEN -> "Open"
        ReportStatus.RESOLVED -> "Resolved"
        ReportStatus.DISMISSED -> "Dismissed"
    }
