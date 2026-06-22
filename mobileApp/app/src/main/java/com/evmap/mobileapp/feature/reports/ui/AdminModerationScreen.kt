package com.evmap.mobileapp.feature.reports.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import core.designsystem.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminModerationScreen(
    state: AdminModerationUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onUpdateStatus: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
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
                    TextButton(onClick = onRefresh) {
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
            item {
                Text("Reports", style = MaterialTheme.typography.titleLarge)
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
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(Spacing.m),
                        verticalArrangement = Arrangement.spacedBy(Spacing.s)
                    ) {
                        Text("Report #${report.id} · ${report.status}", style = MaterialTheme.typography.titleMedium)
                        Text("Event: ${report.eventId} · User: ${report.userId} · Category: ${report.categoryId}")
                        Text(report.description)
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                            Button(onClick = { onUpdateStatus(report.id, "OPEN") }) { Text("Open") }
                            Button(onClick = { onUpdateStatus(report.id, "RESOLVED") }) { Text("Resolved") }
                            TextButton(onClick = { onDelete(report.id) }) { Text("Delete") }
                        }
                    }
                }
            }
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
                    Button(onClick = onSendToAll) { Text("Send to all") }
                    Button(onClick = onSendToUser) { Text("Send to user") }
                }
            }
        }
    }
}
