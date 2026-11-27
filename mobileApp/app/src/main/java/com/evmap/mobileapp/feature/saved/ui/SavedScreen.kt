package feature.saved.ui
import androidx.compose.ui.res.stringResource
import com.evmap.mobileapp.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.evmap.mobileapp.feature.saved.ui.SavedDialog
import com.evmap.mobileapp.feature.saved.ui.SavedUiState
import core.designsystem.Spacing
import feature.saved.ui.components.SavedGroupCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen(
    state: SavedUiState,
    onOpenGroup: (String) -> Unit,
    onAddGroup: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.form_saved_events_f9cdab)) }, navigationIcon = { SavedBackButton(onBack) }) },
        floatingActionButton = {
            if (!state.busy) {
                FloatingActionButton(onClick = onAddGroup) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.form_create_list_13ca88))
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SavedListStatus(state, if (state.groups.isEmpty()) "No saved lists yet" else null, onRetry)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(Spacing.m),
                horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                items(state.groups, key = { it.id }) { list -> SavedGroupCard(list, onOpenGroup) }
            }
        }
    }
}

@Composable
internal fun SavedBackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.form_back_b52b36)) }
}

@Composable
internal fun SavedListStatus(state: SavedUiState, emptyMessage: String?, onRetry: () -> Unit) {
    val loadError = state.loadError
    val writeError = state.writeError.takeIf { state.dialog == null }
    if (!state.loading && loadError == null && writeError == null && emptyMessage == null) return
    Column(
        modifier = Modifier.fillMaxWidth().padding(Spacing.m),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when {
            state.loading -> CircularProgressIndicator()
            loadError != null -> {
                Text(loadError, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetry) { Text(stringResource(R.string.form_retry_9f5cd8)) }
            }
            writeError != null -> Text(writeError, color = MaterialTheme.colorScheme.error)
            emptyMessage != null -> Text(emptyMessage)
        }
    }
}

internal fun eventCountLabel(count: Int) = if (count == 1) "1 event" else "$count events"
