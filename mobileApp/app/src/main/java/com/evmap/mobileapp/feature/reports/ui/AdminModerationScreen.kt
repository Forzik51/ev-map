package com.evmap.mobileapp.feature.reports.ui

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
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Moderation") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onRefresh, enabled = !state.loading) {
                        Text("Refresh")
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
                    TextButton(onClick = onRefresh) { Text("Retry") }
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
                    Text("Email notifications", style = MaterialTheme.typography.titleLarge)
                }
                item {
                    OutlinedTextField(
                        value = state.subject,
                        onValueChange = onSubjectChange,
                        label = { Text("Subject") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = state.body,
                        onValueChange = onBodyChange,
                        label = { Text("Body") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                    )
                }
                item {
                    OutlinedTextField(
                        value = state.userId,
                        onValueChange = onUserIdChange,
                        label = { Text("User ID for direct email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Button(onClick = onSendToAll, enabled = !state.sendingEmail) { Text("Send to all") }
                        Button(onClick = onSendToUser, enabled = !state.sendingEmail) { Text("Send to user") }
                    }
                }
            }
        }
    }
}
