package feature.saved.ui
import androidx.compose.ui.res.stringResource
import com.evmap.mobileapp.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.evmap.mobileapp.feature.saved.ui.SavedDialog
import com.evmap.mobileapp.feature.saved.ui.SavedUiState
import com.evmap.mobileapp.feature.saved.ui.SavedViewModel
import core.designsystem.Spacing

@Composable
internal fun SavedListDialogs(state: SavedUiState, viewModel: SavedViewModel) {
    when (state.dialog) {
        SavedDialog.NAME -> {
            val creating = state.groupItems == null
            AlertDialog(
                onDismissRequest = viewModel::dismissDialog,
                title = { Text(if (creating) stringResource(R.string.form_create_list_13ca88) else stringResource(R.string.form_rename_list_80b1e8)) },
                text = { ListNameInput(state, viewModel::updateName) },
                confirmButton = {
                    Button(onClick = viewModel::saveName, enabled = !state.busy) {
                        Text(if (state.writing) "Saving…" else if (creating) "Create" else "Rename")
                    }
                },
                dismissButton = { TextButton(onClick = viewModel::dismissDialog, enabled = !state.writing) { Text(stringResource(R.string.form_cancel_77dfd2)) } },
            )
        }
        SavedDialog.DELETE -> AlertDialog(
            onDismissRequest = viewModel::dismissDialog,
            title = { Text(stringResource(R.string.form_delete_list_35e899)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    Text("Delete ${state.groupItems?.title.orEmpty()} and its saved entries?")
                    state.writeError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                Button(onClick = viewModel::deleteList, enabled = !state.busy) {
                    Text(if (state.writing) "Deleting…" else stringResource(R.string.form_delete_f6fdbe))
                }
            },
            dismissButton = { TextButton(onClick = viewModel::dismissDialog, enabled = !state.writing) { Text(stringResource(R.string.form_cancel_77dfd2)) } },
        )
        null -> Unit
    }
}

@Composable
internal fun SavedListPicker(state: SavedUiState, viewModel: SavedViewModel, onClose: () -> Unit) {
    Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp) {
        Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            val saved = state.savedTo
            if (saved != null) {
                Text(stringResource(R.string.form_event_saved_236441), style = MaterialTheme.typography.headlineSmall)
                Text("Added to $saved")
                Button(onClick = onClose) { Text(stringResource(R.string.form_done_e9b450)) }
            } else if (state.dialog == SavedDialog.NAME) {
                Text(stringResource(R.string.form_create_list_13ca88), style = MaterialTheme.typography.headlineSmall)
                ListNameInput(state, viewModel::updateName)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    TextButton(onClick = viewModel::dismissDialog, enabled = !state.writing) { Text(stringResource(R.string.form_cancel_77dfd2)) }
                    Button(onClick = viewModel::saveName, enabled = !state.busy) {
                        Text(if (state.writing) "Creating…" else "Create")
                    }
                }
            } else {
                Text(stringResource(R.string.form_save_to_list_7931f7), style = MaterialTheme.typography.headlineSmall)
                SavedListStatus(state, if (state.groups.isEmpty()) "Create a list to save this event." else null, viewModel::load)
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                    items(state.groups, key = { it.id }) { list ->
                        ListItem(
                            headlineContent = { Text(list.name) },
                            supportingContent = { Text(eventCountLabel(list.count)) },
                            modifier = Modifier.clickable(enabled = !state.busy) { viewModel.saveEvent(list) },
                        )
                    }
                }
                if (state.writing) Text(stringResource(R.string.form_saving_event_860980))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    TextButton(onClick = onClose, enabled = !state.writing) { Text(stringResource(R.string.form_cancel_77dfd2)) }
                    TextButton(onClick = viewModel::openNameDialog, enabled = !state.busy) { Text(stringResource(R.string.form_create_list_13ca88)) }
                }
            }
        }
    }
}

@Composable
private fun ListNameInput(state: SavedUiState, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = state.nameDraft,
        onValueChange = onChange,
        label = { Text(stringResource(R.string.form_label_4ad268)) },
        singleLine = true,
        enabled = !state.writing,
        isError = state.writeError != null,
        supportingText = state.writeError?.let { { Text(it) } },
        modifier = Modifier.fillMaxWidth(),
    )
}
