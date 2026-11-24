package com.evmap.mobileapp.feature.events.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.evmap.mobileapp.feature.feed.data.toAbsoluteMediaUrl
import core.designsystem.Spacing
import core.ui.components.CategoryChip
import core.ui.components.ChipsFlowRow
import core.ui.components.DateField
import core.ui.components.LocationField
import feature.events.ui.components.MediaHeader
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private enum class DateTarget { START, END }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventFormScreen(
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    onOpenCatalog: () -> Unit,
    viewModel: EventFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) {
        viewModel.addPhotos(it)
    }
    var dateTarget by rememberSaveable { mutableStateOf<DateTarget?>(null) }
    var showLocations by rememberSaveable { mutableStateOf(false) }
    var categoryQuery by rememberSaveable { mutableStateOf("") }
    val fieldsEditable = state.savedEventId == null && !state.creationUnconfirmed

    fun closeForm() {
        if (state.saving) return
        if (state.savedEventId != null) viewModel.finishWithSavedPhotos() else onBack()
    }

    BackHandler(enabled = state.saving || state.savedEventId != null) { closeForm() }
    LaunchedEffect(state.finishedEventId) { state.finishedEventId?.let(onSaved) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.editing) "Edit event" else "Create event") },
                navigationIcon = {
                    IconButton(onClick = ::closeForm, enabled = !state.saving) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            if (state.ready) {
                Column(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(Spacing.m),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (state.savedEventId != null && !state.saving) {
                        TextButton(onClick = viewModel::finishWithSavedPhotos) { Text("Open event") }
                    }
                    Button(
                        onClick = if (state.creationUnconfirmed) onOpenCatalog else viewModel::save,
                        enabled = !state.saving,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            when {
                                state.saving -> "Saving…"
                                state.creationUnconfirmed -> "Check All events"
                                state.savedEventId != null -> "Retry photo changes"
                                state.editing -> "Save changes"
                                else -> "Publish event"
                            }
                        )
                    }
                }
            }
        },
    ) { padding ->
        if (!state.ready) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                if (state.loading) CircularProgressIndicator()
                else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = viewModel::load) { Text("Retry") }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.m),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                val existing = state.existingPhotos.filterNot { it.id in state.photosToDelete }
                val media = existing.mapNotNull { photo ->
                    photo.imagePath.toAbsoluteMediaUrl()?.let { url -> photo.id to url }
                }
                val canChangePhotos = !state.saving && !state.creationUnconfirmed
                MediaHeader(
                    media = media.map { it.second } + state.pendingPhotos.map { it.uri.toString() },
                    onAddClick = if (canChangePhotos) {
                        { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                    } else null,
                    onRemoveAt = if (canChangePhotos) {
                        { index ->
                            if (index < media.size) viewModel.removeExistingPhoto(media[index].first)
                            else state.pendingPhotos.getOrNull(index - media.size)?.let { viewModel.removePendingPhoto(it.uploadId) }
                        }
                    } else null,
                )
                Text("JPEG, PNG or WebP · Up to 5 MB per photo", style = MaterialTheme.typography.bodySmall)
                if (state.photosToDelete.isNotEmpty()) {
                    TextButton(onClick = viewModel::undoPhotoRemovals, enabled = canChangePhotos) {
                        Text("Undo photo removals")
                    }
                }
                val failedPhotos = state.pendingPhotos.mapIndexedNotNull { index, photo ->
                    photo.error?.let { "Photo ${media.size + index + 1}: $it" }
                }
                failedPhotos.forEach { Text(it, color = MaterialTheme.colorScheme.error) }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (state.savedEventId != null) {
                    Text("Event saved. Photo changes still pending: ${state.pendingPhotos.size + state.photosToDelete.size}.")
                }
                if (fieldsEditable) {
                    OutlinedTextField(
                        value = state.title,
                        onValueChange = viewModel::updateTitle,
                        label = { Text("Title") },
                        singleLine = true,
                        enabled = !state.saving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.description,
                        onValueChange = viewModel::updateDescription,
                        label = { Text("Description") },
                        minLines = 3,
                        enabled = !state.saving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    DateField(
                        value = state.startsAt.displayDate(),
                        label = "Start date",
                        placeholder = "Select start date",
                        onClick = { if (!state.saving) dateTarget = DateTarget.START },
                    )
                    DateField(
                        value = state.endsAt.displayDate(),
                        label = "End date (optional)",
                        placeholder = "Select end date",
                        onClick = { if (!state.saving) dateTarget = DateTarget.END },
                    )
                    if (state.endsAt != null) {
                        TextButton(onClick = viewModel::clearEndDate, enabled = !state.saving) {
                            Text("Remove end date")
                        }
                    }
                    LocationField(
                        value = state.locationName,
                        label = "Location",
                        placeholder = "Select location",
                        onClick = { if (!state.saving) showLocations = true },
                    )
                    OutlinedTextField(
                        value = categoryQuery,
                        onValueChange = { categoryQuery = it },
                        label = { Text("Filter categories") },
                        singleLine = true,
                        enabled = !state.saving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val categories = state.categories.filter { it.name.contains(categoryQuery.trim(), ignoreCase = true) }
                    ChipsFlowRow {
                        categories.forEach { category ->
                            CategoryChip(
                                text = category.name,
                                selected = category.id in state.categoryIds,
                                onToggle = { viewModel.toggleCategory(category.id) },
                            )
                        }
                    }
                    if (categories.isEmpty()) Text("No matching categories")
                }
            }
        }
    }

    dateTarget?.let { target ->
        EventDateDialog(
            selected = if (target == DateTarget.START) state.startsAt else state.endsAt ?: state.startsAt,
            onDismiss = { dateTarget = null },
            onSelect = { date ->
                if (target == DateTarget.START) viewModel.updateStartDate(date) else viewModel.updateEndDate(date)
                dateTarget = null
            },
        )
    }

    if (showLocations) {
        AlertDialog(
            onDismissRequest = { showLocations = false },
            title = { Text("Select location") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    OutlinedTextField(
                        value = state.locationQuery,
                        onValueChange = viewModel::searchLocation,
                        label = { Text("Search locations") },
                        singleLine = true,
                    )
                    when {
                        state.locationSearching -> CircularProgressIndicator()
                        state.locationError != null -> {
                            Text(state.locationError.orEmpty(), color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = { viewModel.searchLocation(state.locationQuery) }) { Text("Retry") }
                        }
                        state.locationQuery.trim().length < 2 -> Text("Enter at least 2 characters")
                        state.locationResults.isEmpty() -> Text("No locations found")
                        else -> LazyColumn(Modifier.heightIn(max = 300.dp)) {
                            items(state.locationResults, key = { it.id }) { location ->
                                ListItem(
                                    headlineContent = { Text(location.name) },
                                    trailingContent = {
                                        TextButton(onClick = {
                                            viewModel.selectLocation(location)
                                            showLocations = false
                                        }) { Text("Choose") }
                                    },
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLocations = false }) { Text("Close") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventDateDialog(selected: Instant?, onDismiss: () -> Unit, onSelect: (LocalDate) -> Unit) {
    val initialMillis = selected?.atZone(ZoneId.systemDefault())?.toLocalDate()
        ?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    val picker = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = picker.selectedDateMillis != null,
                onClick = {
                    picker.selectedDateMillis?.let { onSelect(Instant.ofEpochMilli(it).atOffset(ZoneOffset.UTC).toLocalDate()) }
                },
            ) { Text("Select") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = picker)
    }
}

private fun Instant?.displayDate(): String =
    this?.atZone(ZoneId.systemDefault())?.toLocalDate()?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)).orEmpty()
