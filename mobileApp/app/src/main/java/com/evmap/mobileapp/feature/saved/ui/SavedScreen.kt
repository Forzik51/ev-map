package feature.saved.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import core.designsystem.AppTheme
import core.designsystem.Spacing
import core.ui.model.SavedGroupUi
import feature.saved.ui.components.SavedGroupCard

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun SavedScreenPreview() {
    val sampleGroups = listOf(
        SavedGroupUi(
            id = "1",
            name = "Name",
            count = 5,
            imageUrl = null
        ),
        SavedGroupUi(
            id = "2", 
            name = "Name",
            count = 3,
            imageUrl = null
        ),
        SavedGroupUi(
            id = "3",
            name = "Name",
            count = 8,
            imageUrl = null
        ),
        SavedGroupUi(
            id = "4",
            name = "Name", 
            count = 2,
            imageUrl = null
        )
    )

    AppTheme {
        SavedScreen(
            groups = sampleGroups,
            onOpenGroup = {},
            onAddGroup = {},
            onBack = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen(
    groups: List<SavedGroupUi>,
    onOpenGroup: (String) -> Unit,
    onAddGroup: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Back",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Saved events",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
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
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddGroup,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Group"
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = Spacing.s),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.s)
        ) {
            items(
                items = groups,
                key = { it.id }
            ) { group ->
                SavedGroupCard(
                    group = group,
                    onClick = onOpenGroup
                )
            }
        }
    }
}
