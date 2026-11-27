package feature.explore.ui
import androidx.compose.ui.res.stringResource
import com.evmap.mobileapp.R

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
    onTagsChange: (String)->Unit = {},
    onRetryCategories: () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    val categoryOptions: List<Pair<Long?, String>> = listOf(null to stringResource(R.string.form_all_categories_060be0)) +
        categories.map { it.id to it.name }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            Text(stringResource(R.string.form_event_filters_884913), style = MaterialTheme.typography.titleLarge)
            FilterDropdown(
                label = stringResource(R.string.form_sort_by_a2a5bd),
                value = filters.sort.label,
                options = ExploreSort.entries.map { it to it.label },
                onSelect = onSortChange,
            )
            FilterDropdown(
                label = stringResource(R.string.form_category_a3c686),
                value = categoryOptions.firstOrNull { it.first == filters.categoryId }?.second ?: stringResource(R.string.form_selected_category_fb4f11),
                options = categoryOptions,
                onSelect = onCategoryChange,
            )
            if (loadingCategories) {
                CircularProgressIndicator()
            } else if (categoryLoadFailed) {
                Text(stringResource(R.string.form_could_not_load_categories_d1a974), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetryCategories) { Text(stringResource(R.string.form_retry_9f5cd8)) }
            }
            OutlinedTextField(filters.tagsText,onTagsChange,label={ Text(stringResource(R.string.form_label_d92fa5)) },supportingText={Text(stringResource(R.string.form_5_79f182))},modifier=Modifier.fillMaxWidth())
            PrimaryButton(text = stringResource(R.string.form_apply_cfea41), onClick = onApply, modifier = Modifier.fillMaxWidth())
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
