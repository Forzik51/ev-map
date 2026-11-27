package com.evmap.mobileapp.feature.reports.ui
import androidx.compose.ui.res.stringResource
import com.evmap.mobileapp.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.evmap.mobileapp.core.data.dto.ReportStatus
import core.designsystem.Spacing

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdminModerationScreen(
    state: AdminModerationUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onFilter: (ReportStatus) -> Unit,
    onOpenReport: (Long) -> Unit,
    onSubjectChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onUserIdChange: (String) -> Unit,
    onSendToAll: () -> Unit,
    onSendToUser: () -> Unit,
    modifier: Modifier = Modifier,
    onCategoryDecision: (Long,Boolean)->Unit = {_,_->},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.form_moderation_28f3eb)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.form_back_b52b36))
                    }
                },
                actions = {
                    TextButton(onClick = onRefresh, enabled = !state.loading) {
                        Text(stringResource(R.string.form_refresh_56e3ba))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.m)
        ) {
            if (!state.accessDenied) {
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        ReportStatus.entries.forEach { status ->
                            FilterChip(
                                selected = status == state.filter,
                                onClick = { onFilter(status) },
                                label = { Text(status.label) },
                            )
                        }
                    }
                }
            }
            if (state.loading) {
                item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            }
            state.loadError?.let { error ->
                item {
                    Text(error, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRefresh) { Text(stringResource(R.string.form_retry_9f5cd8)) }
                }
            }
            if (!state.loading && state.loadError == null && state.reports.isEmpty()) {
                item { Text("No ${state.filter.label.lowercase()} reports") }
            }
            if (!state.error.isNullOrBlank()) {
                item {
                    Text(state.error, color = MaterialTheme.colorScheme.error)
                }
            }
            if (!state.message.isNullOrBlank()) {
                item {
                    Text(state.message, color = MaterialTheme.colorScheme.primary)
                }
            }
            if(state.categoryRequests.isNotEmpty()) item { Text(stringResource(R.string.form_label_4f7a56),style=MaterialTheme.typography.titleMedium) }
            items(state.categoryRequests,key={"category-${it.id}"}) { request ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(Spacing.m)) {
                        Text(request.name)
                        Row {
                            TextButton(onClick={request.id?.let {onCategoryDecision(it,true)}}) {Text(stringResource(R.string.form_label_6842aa))}
                            TextButton(onClick={request.id?.let {onCategoryDecision(it,false)}}) {Text(stringResource(R.string.form_label_50aa49))}
                        }
                    }
                }
            }
            items(state.reports, key = { it.id }) { report ->
                Card(onClick = { onOpenReport(report.id) }, modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(Spacing.m),
                        verticalArrangement = Arrangement.spacedBy(Spacing.s)
                    ) {
                        Text(report.eventName, style = MaterialTheme.typography.titleMedium)
                        Text("${report.categoryName} · ${report.status.label}")
                        Text(report.description)
                    }
                }
            }
            if (!state.accessDenied) {
                item {
                    Spacer(Modifier.height(Spacing.m))
                    Text(stringResource(R.string.form_email_notifications_b7b0bc), style = MaterialTheme.typography.titleLarge)
                }
                item {
                    OutlinedTextField(
                        value = state.subject,
                        onValueChange = onSubjectChange,
                        label = { Text(stringResource(R.string.form_subject_8d183d)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = state.body,
                        onValueChange = onBodyChange,
                        label = { Text(stringResource(R.string.form_body_718a7e)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                    )
                }
                item {
                    OutlinedTextField(
                        value = state.userId,
                        onValueChange = onUserIdChange,
                        label = { Text(stringResource(R.string.form_user_id_for_direct_email_75a4c2)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Button(onClick = onSendToAll, enabled = !state.sendingEmail) { Text(stringResource(R.string.form_send_to_all_b0b1fd)) }
                        Button(onClick = onSendToUser, enabled = !state.sendingEmail) { Text(stringResource(R.string.form_send_to_user_9f55b0)) }
                    }
                }
            }
        }
    }
}
