package feature.explore.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.evmap.mobileapp.core.data.dto.EventCategoryDto
import core.designsystem.Spacing
import core.ui.components.PrimaryButton
import feature.explore.ExploreFilters
import feature.explore.ExploreSort

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreFilterSheet(
    filters: ExploreFilters,
    categories: List<EventCategoryDto>,
    loadingCategories: Boolean,
    categoryLoadFailed: Boolean,
    onSortChange: (ExploreSort) -> Unit,
    onCategoryChange: (Long?) -> Unit,
    onRetryCategories: () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    val categoryOptions: List<Pair<Long?, String>> = listOf(null to "All categories") +
        categories.map { it.id to it.name }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            Text("Event filters", style = MaterialTheme.typography.titleLarge)
            FilterDropdown(
                label = "Sort by",
                value = filters.sort.label,
                options = ExploreSort.entries.map { it to it.label },
                onSelect = onSortChange,
            )
            FilterDropdown(
                label = "Category",
                value = categoryOptions.firstOrNull { it.first == filters.categoryId }?.second ?: "Selected category",
                options = categoryOptions,
                onSelect = onCategoryChange,
            )
            if (loadingCategories) {
                CircularProgressIndicator()
            } else if (categoryLoadFailed) {
                Text("Could not load categories.", color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetryCategories) { Text("Retry") }
            }
            PrimaryButton(text = "Apply", onClick = onApply, modifier = Modifier.fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> FilterDropdown(
    label: String,
    value: String,
    options: List<Pair<T, String>>,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (option, name) ->
                DropdownMenuItem(text = { Text(name) }, onClick = {
                    onSelect(option)
                    expanded = false
                })
            }
        }
    }
}
