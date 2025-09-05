package feature.events.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import core.designsystem.AppTheme
import core.designsystem.Spacing
import core.ui.components.*
import core.ui.model.EventDraftUi
import feature.events.ui.components.MediaHeader


@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun EventCreateScreenPreview() {
    // Sample draft data for preview
    var title by remember { mutableStateOf("Street Food Fest") }
    var subtitle by remember { mutableStateOf("Praga-Północ") }
    var description by remember { mutableStateOf("Best food trucks, live music, kids zone.") }
    var date by remember { mutableStateOf("Aug 30, 2025") }
    var location by remember { mutableStateOf("Warsaw") }
    var categories by remember { mutableStateOf(setOf("Food", "Music")) }
    var categoriesQuery by remember { mutableStateOf("") }
    var media by remember { mutableStateOf(listOf<String>()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val draft = EventDraftUi(
        title = title,
        subtitle = subtitle,
        description = description,
        date = date,
        location = location,
        categories = categories,
        availableCategories = listOf("Food", "Music", "Art", "Kids", "Outdoors"),
        media = media,
        categoriesQuery = categoriesQuery
    )

    AppTheme {
        EventCreateScreen(
            draft = draft,
            showDatePicker = showDatePicker,
            onTitleChange = { title = it },
            onSubtitleChange = { subtitle = it },
            onDescriptionChange = { description = it },
            onPickDate = { showDatePicker = true },
            onDateSelected = { millis ->
                // quick formatting for preview; replace with your formatter
                date = millis?.toString() ?: date
            },
            onDatePickerDismiss = { showDatePicker = false },
            onPickLocation = { location = "Selected on map" },
            onCategoriesQueryChange = { categoriesQuery = it },
            onToggleCategory = { cat ->
                categories = if (cat in categories) categories - cat else categories + cat
            },
            onAddPhoto = { media = media + "https://picsum.photos/400/200?${media.size}" },
            onRemovePhoto = { index ->
                media = media.toMutableList().also { if (index in it.indices) it.removeAt(index) }
            },
            onSubmit = { /* no-op in preview */ },
            onBack = { /* no-op in preview */ },
            modifier = Modifier
        )
    }
}









@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventCreateScreen(
    draft: EventDraftUi,
    showDatePicker: Boolean = false,
    onTitleChange: (String) -> Unit,
    onSubtitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPickDate: () -> Unit,
    onDateSelected: (Long?) -> Unit = {},
    onDatePickerDismiss: () -> Unit = {},
    onPickLocation: () -> Unit,
    onCategoriesQueryChange: (String) -> Unit,
    onToggleCategory: (String) -> Unit,
    onAddPhoto: () -> Unit,
    onRemovePhoto: (Int) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val datePickerState = rememberDatePickerState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Back",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        bottomBar = {
            PrimaryButton(
                text = "Add",
                onClick = onSubmit,
                modifier = Modifier.padding(Spacing.m),
                enabled = draft.title.isNotBlank()
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.m)
        ) {
            // Media Header with proper spacing
            MediaHeader(
                media = draft.media,
                onAddClick = onAddPhoto,
                onRemoveAt = onRemovePhoto
            )

            // Title and Subtitle Headers
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.m)
            ) {
                Text(
                    text = "Title",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(Spacing.xs))

                Text(
                    text = "Subtitle",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Description Text (Display)
            Text(
                text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor. Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor. Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor. Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor. Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.m)
            )

            // Form Fields with consistent spacing
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.m),
                verticalArrangement = Arrangement.spacedBy(Spacing.m)
            ) {
                // Title Field (hidden in UI but keeping for functionality)
                OutlinedTextField(
                    value = draft.title,
                    onValueChange = onTitleChange,
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                // Subtitle Field (hidden in UI but keeping for functionality)
                OutlinedTextField(
                    value = draft.subtitle,
                    onValueChange = onSubtitleChange,
                    label = { Text("Subtitle") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                // Description Field (hidden in UI but keeping for functionality)
                OutlinedTextField(
                    value = draft.description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                // Date Field - displays the chosen date from draft
                DateField(
                    value = draft.date.ifEmpty { "08/17/2025" },
                    label = "Date",
                    onClick = onPickDate,
                    placeholder = "Select date"
                )

                // Location Field
                LocationField(
                    value = draft.location.ifEmpty { "Warsaw" },
                    onClick = onPickLocation,
                    label = "Location",
                    placeholder = "Select location"
                )
            }

            // Categories Search and Selection Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.m),
                verticalArrangement = Arrangement.spacedBy(Spacing.m)
            ) {
                // Interactive Search Field for Categories
                SearchField(
                    value = draft.categoriesQuery,
                    onValueChange = onCategoriesQueryChange,
                    placeholder = "Search categories",
                    onSearch = { /* Optional: trigger search action */ }
                )

                // Filter available categories based on search query
                val filteredCategories = if (draft.categoriesQuery.isBlank()) {
                    // Show default categories when no search query
                    listOf("Category") + draft.availableCategories
                } else {
                    // Filter categories based on search query
                    draft.availableCategories.filter { category ->
                        category.contains(draft.categoriesQuery, ignoreCase = true)
                    }
                }

                // Category Chips with proper Material3 styling
                if (filteredCategories.isNotEmpty()) {
                    ChipsFlowRow {
                        filteredCategories.forEach { category ->
                            CategoryChip(
                                text = category,
                                selected = category in draft.categories,
                                onToggle = { onToggleCategory(category) }
                            )
                        }
                    }
                } else if (draft.categoriesQuery.isNotBlank()) {
                    // Show "No categories found" message when search returns no results
                    Text(
                        text = "No categories found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(Spacing.s)
                    )
                }
            }

            // Bottom spacing to ensure content doesn't get hidden behind bottom bar
            Spacer(modifier = Modifier.height(Spacing.xxl))
        }
    }

    // Material3 DatePicker Dialog - controlled by showDatePicker parameter
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = onDatePickerDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        onDateSelected(datePickerState.selectedDateMillis)
                        onDatePickerDismiss()
                    }
                ) {
                    Text(
                        text = "OK",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDatePickerDismiss) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            modifier = Modifier.padding(Spacing.m)
        ) {
            DatePicker(
                state = datePickerState,
                modifier = Modifier.padding(Spacing.m),
                title = {
                    Text(
                        text = "Select date",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(Spacing.m)
                    )
                }
            )
        }
    }
}
