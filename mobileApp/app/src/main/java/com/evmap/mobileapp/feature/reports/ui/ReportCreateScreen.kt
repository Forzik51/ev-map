package com.evmap.mobileapp.feature.reports.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import core.designsystem.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportCreateScreen(
    state: ReportCreateUiState,
    onBack: () -> Unit,
    onSelectCategory: (Long) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onRetryCategories: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Report event") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (state.loadingCategories && state.categories.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(Spacing.m),
                verticalArrangement = Arrangement.spacedBy(Spacing.m)
            ) {
                item {
                    Text("Category", style = MaterialTheme.typography.titleMedium)
                }
                if (state.categories.isEmpty()) {
                    item {
                        Text(
                            state.categoryError ?: "No report categories are available.",
                            color = MaterialTheme.colorScheme.error,
                        )
                        TextButton(onClick = onRetryCategories) { Text("Retry categories") }
                    }
                }
                items(state.categories, key = { it.id }) { category ->
                    FilterChip(
                        selected = state.selectedCategoryId == category.id,
                        enabled = !state.submitting,
                        onClick = { onSelectCategory(category.id) },
                        label = { Text(category.name) }
                    )
                }
                item {
                    OutlinedTextField(
                        value = state.description,
                        onValueChange = onDescriptionChange,
                        enabled = !state.submitting,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Description") },
                        supportingText = { Text("${state.description.trim().length}/100") },
                        minLines = 4,
                    )
                }
                if (!state.error.isNullOrBlank()) {
                    item {
                        Text(
                            text = state.error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                item {
                    Spacer(Modifier.height(Spacing.s))
                    Button(
                        onClick = onSubmit,
                        enabled = !state.submitting && !state.submitted && state.categories.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (state.submitting) "Submitting…" else "Submit report")
                    }
                }
            }
        }
    }
}
