package feature.explore.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import core.designsystem.AppTheme
import core.designsystem.Spacing
import core.ui.components.PrimaryButton

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun ExploreFilterSheetPreview() {
    var sortBy by remember { mutableStateOf("Popular") }
    var type by remember { mutableStateOf<String?>(null) }

    val sortOptions = listOf("Popular", "Newest", "Top rated", "Nearest")
    val typeOptions = listOf("All", "Music", "Art", "Food", "Family")

    AppTheme {
        // Preview the CONTENT (not the bottom sheet) so it’s visible
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            ExploreFilterSheetContent(
                sortBy = sortBy,
                sortOptions = sortOptions,
                type = type,
                typeOptions = typeOptions,
                onSortChange = { sortBy = it },
                onTypeChange = { type = it },
                onApply = { /* no-op */ },
                onDismiss = { /* no-op */ }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreFilterSheet(
    sortBy: String,
    sortOptions: List<String>,
    type: String?,
    typeOptions: List<String>,
    onSortChange: (String) -> Unit,
    onTypeChange: (String) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        ExploreFilterSheetContent(
            sortBy = sortBy,
            sortOptions = sortOptions,
            type = type,
            typeOptions = typeOptions,
            onSortChange = onSortChange,
            onTypeChange = onTypeChange,
            onApply = onApply,
            onDismiss = onDismiss
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExploreFilterSheetContent(
    sortBy: String,
    sortOptions: List<String>,
    type: String?,
    typeOptions: List<String>,
    onSortChange: (String) -> Unit,
    onTypeChange: (String) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    var sortExpanded by rememberSaveable { mutableStateOf(false) }
    var typeExpanded by rememberSaveable { mutableStateOf(false) }

    // Fallback options if lists are empty
    val actualSortOptions = sortOptions.ifEmpty {
        listOf("Menu item 1", "Menu item 2", "Menu item 3", "Menu item 4")
    }
    val actualTypeOptions = typeOptions.ifEmpty {
        listOf("Menu item 1", "Menu item 2", "Menu item 3", "Menu item 4")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Back",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.l)
        ) {
            // Filter Controls
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.l)
            ) {
                Spacer(modifier = Modifier.height(Spacing.m))

                // Sort by dropdown
                ExposedDropdownMenuBox(
                    expanded = sortExpanded,
                    onExpandedChange = { sortExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = sortBy,
                        onValueChange = { },
                        readOnly = true,
                        label = {
                            Text(
                                text = "Sort by",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = sortExpanded)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = sortExpanded,
                        onDismissRequest = { sortExpanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        actualSortOptions.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                onClick = {
                                    onSortChange(option)
                                    sortExpanded = false
                                },
                                modifier = Modifier.height(48.dp)
                            )
                        }
                    }
                }

                // Type dropdown
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = type ?: "type",
                        onValueChange = { },
                        readOnly = true,
                        label = {
                            Text(
                                text = "type",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        actualTypeOptions.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                onClick = {
                                    onTypeChange(option)
                                    typeExpanded = false
                                },
                                modifier = Modifier.height(48.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Apply Button
            Column {
                PrimaryButton(
                    text = "Apply",
                    onClick = {
                        onApply()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(Spacing.xl))
            }
        }
    }
}
