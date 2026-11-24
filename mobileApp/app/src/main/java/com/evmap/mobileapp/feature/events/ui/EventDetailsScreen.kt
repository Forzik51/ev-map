package com.evmap.mobileapp.feature.events.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.evmap.mobileapp.core.data.dto.EventCommentDto
import com.evmap.mobileapp.feature.feed.data.toAbsoluteMediaUrl
import core.designsystem.Spacing
import core.ui.components.AppBottomBar
import core.ui.components.ChipsFlowRow
import feature.events.ui.components.MediaHeader
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailsScreen(
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleted: () -> Unit,
    onReport: () -> Unit,
    onSave: () -> Unit,
    onNavigate: (String) -> Unit,
    refreshAfterEdit: Boolean,
    onRefreshConsumed: () -> Unit,
    viewModel: EventViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val event = state.event
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = state.deleting) { }

    LaunchedEffect(Unit) {
        if (!refreshAfterEdit) viewModel.load()
    }
    LaunchedEffect(refreshAfterEdit) {
        if (refreshAfterEdit) {
            viewModel.load()
            onRefreshConsumed()
        }
    }
    LaunchedEffect(state.deleted) { if (state.deleted) onDeleted() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(event?.name ?: "Event", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !state.deleting) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (event != null) {
                        IconButton(onClick = onSave, enabled = !state.deleting) {
                            Icon(Icons.Outlined.BookmarkAdd, contentDescription = "Save event")
                        }
                        IconButton(onClick = viewModel::share, enabled = !state.sharing && !state.deleting) {
                            Icon(Icons.Default.Share, contentDescription = "Share event")
                        }
                        IconButton(onClick = onReport, enabled = !state.deleting) {
                            Icon(Icons.Default.Report, contentDescription = "Report event")
                        }
                        if (state.canManage) {
                            Box {
                                IconButton(onClick = { menuOpen = true }, enabled = !state.deleting) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Manage event")
                                }
                                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                    DropdownMenuItem(text = { Text("Edit event") }, onClick = {
                                        menuOpen = false
                                        onEdit()
                                    })
                                    DropdownMenuItem(text = { Text("Delete event") }, onClick = {
                                        menuOpen = false
                                        confirmDelete = true
                                    })
                                }
                            }
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (!state.deleting) AppBottomBar(currentRoute = null, onNavigate = onNavigate)
        },
    ) { padding ->
        if (event == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                if (state.loading) CircularProgressIndicator()
                else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.loadError.orEmpty(), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = viewModel::load) { Text("Retry") }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(Spacing.m),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                val media = state.photos.mapNotNull { it.imagePath.toAbsoluteMediaUrl() }
                    .ifEmpty { listOfNotNull(event.imagePath.toAbsoluteMediaUrl()) }
                if (media.isNotEmpty()) item { MediaHeader(media = media) }
                state.photosError?.let { error ->
                    item {
                        Text(error, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = viewModel::loadPhotos) { Text("Retry photos") }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        event.locationName?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
                        Text(event.startsAt.displayDateTime())
                        event.endsAt?.let { Text("Ends ${it.displayDateTime()}") }
                        Text(event.description)
                        if (event.categories.isNotEmpty()) {
                            ChipsFlowRow {
                                event.categories.forEach { category ->
                                    Text(
                                        category.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.padding(Spacing.xs),
                                    )
                                }
                            }
                        }
                        Text("${event.repostsCount} shares", style = MaterialTheme.typography.bodySmall)
                    }
                }
                item {
                    RatingSection(
                        average = event.ratingAvg,
                        count = event.ratingsCount,
                        selected = event.myRating,
                        enabled = !state.savingRating && !state.deleting,
                        onRate = viewModel::rate,
                    )
                    if (state.savingRating) Text("Saving rating…")
                    if (state.deleting) Text("Deleting event…")
                    state.actionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    state.loadError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = viewModel::load) { Text("Refresh event") }
                    }
                }
                item {
                    HorizontalDivider()
                    Text("Comments (${event.commentsCount})", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = state.commentText,
                        onValueChange = viewModel::updateComment,
                        label = { Text("Write a comment") },
                        supportingText = { Text("${state.commentText.codePointCount(0, state.commentText.length)}/100") },
                        enabled = !state.sendingComment && !state.deleting,
                        isError = state.commentError != null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    state.commentError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(
                        onClick = viewModel::sendComment,
                        enabled = state.commentText.isNotBlank() && !state.sendingComment && !state.deleting,
                    ) { Text(if (state.sendingComment) "Sending…" else "Post comment") }
                }
                items(state.comments, key = { it.id }) { comment -> CommentRow(comment) }
                item {
                    when {
                        state.loadingComments -> CircularProgressIndicator()
                        state.commentsError != null -> {
                            Text(state.commentsError.orEmpty(), color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = { viewModel.loadComments() }) { Text("Retry comments") }
                        }
                        state.comments.isEmpty() -> Text("No comments yet")
                        state.nextCommentPage != null -> TextButton(onClick = { viewModel.loadComments() }) {
                            Text("Load more comments")
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete && event != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete event?") },
            text = { Text("Delete ${event.name} and its photos and comments?") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun RatingSection(average: Double?, count: Int, selected: Double?, enabled: Boolean, onRate: (Int) -> Unit) {
    Text(
        average?.let { "Rating ${it.displayRating()} / 5 · $count ratings" } ?: "No ratings yet",
        style = MaterialTheme.typography.titleMedium,
    )
    Text(selected?.let { "Your rating ${it.displayRating()} / 5" } ?: "Rate this event")
    Row {
        (1..5).forEach { rating ->
            val filled = selected != null && rating <= selected
            IconButton(onClick = { onRate(rating) }, enabled = enabled) {
                Icon(
                    if (filled) Icons.Default.Star else Icons.Outlined.StarOutline,
                    contentDescription = "Rate $rating out of 5",
                    tint = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CommentRow(comment: EventCommentDto) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(comment.username ?: "User", style = MaterialTheme.typography.labelLarge)
        Text(comment.commentAt.displayDateTime(), style = MaterialTheme.typography.bodySmall)
        Text(comment.text)
    }
}

internal fun String.displayDateTime(): String = runCatching {
    Instant.parse(this).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))
}.getOrDefault(this)
