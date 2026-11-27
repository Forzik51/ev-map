package feature.map.ui
import androidx.compose.ui.res.stringResource
import com.evmap.mobileapp.R

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.evmap.mobileapp.feature.map.ui.MapCameraCommand
import com.evmap.mobileapp.feature.map.ui.MapUiState
import core.designsystem.Spacing
import core.ui.components.AppBottomBar
import core.ui.components.SearchField
import core.ui.model.MapCameraUi
import core.ui.model.MapPinUi
import core.ui.model.MapViewport
import feature.map.ui.components.MapControls
import feature.map.ui.components.MapEventPreviewCard
import kotlinx.coroutines.flow.Flow

@Composable
fun SearchMapScreen(
    state: MapUiState,
    initialCamera: MapCameraUi,
    cameraCommands: Flow<MapCameraCommand>,
    onViewport: (MapCameraUi, MapViewport) -> Unit,
    onQueryChange: (String) -> Unit,
    onPinClick: (Set<String>) -> Unit,
    onPinChoice: (MapPinUi) -> Unit,
    onClusterClick: (Double, Double) -> Unit,
    onRecenter: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onMyLocation: () -> Unit,
    locating: Boolean,
    locationNotice: String?,
    onLocationSettings: (() -> Unit)?,
    onDismissLocationNotice: () -> Unit,
    onRetry: () -> Unit,
    onOpenEvent: (String) -> Unit,
    onDismissPreview: () -> Unit,
    onNavigate: (String) -> Unit,
    onApplyFilters: (Long?,String)->Unit = {_,_->},
) {
    var showFilters by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<Long?>(null) }
    var filterTags by remember { mutableStateOf("") }
    var styleAttempt by remember { mutableIntStateOf(0) }
    var styleStatus by remember { mutableStateOf(MapStyleStatus.LOADING) }
    Scaffold(bottomBar = { AppBottomBar(currentRoute = "map", onNavigate = onNavigate) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            MapSurface(
                initialCamera = initialCamera,
                pins = state.pins,
                clusters = state.clusters,
                location = state.location,
                commands = cameraCommands,
                styleAttempt = styleAttempt,
                onStyleStatus = { styleStatus = it },
                onViewport = onViewport,
                onPins = onPinClick,
                onCluster = onClusterClick,
                onBackground = onDismissPreview,
                modifier = Modifier.fillMaxSize().testTag("map_surface").semantics {
                    stateDescription = when (styleStatus) {
                        MapStyleStatus.LOADING -> "Map loading"
                        MapStyleStatus.READY -> "Map ready"
                        MapStyleStatus.ERROR -> "Map load failed"
                    }
                },
            )
            Column(
                modifier = Modifier.align(Alignment.TopCenter).padding(Spacing.m),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                SearchField(state.query, onQueryChange, "Search events", onSearch = onRetry)
                androidx.compose.material3.TextButton(onClick={selectedCategory=state.categoryId;filterTags=state.tagsText;showFilters=true}) { androidx.compose.material3.Text(stringResource(R.string.form_label_f0681c)) }
                if(showFilters) androidx.compose.material3.AlertDialog(onDismissRequest={showFilters=false},title={androidx.compose.material3.Text(stringResource(R.string.form_label_196688))},text={
                    Column(Modifier.heightIn(max=320.dp).verticalScroll(rememberScrollState())) {
                        androidx.compose.material3.FilterChip(selectedCategory==null,{selectedCategory=null},label={androidx.compose.material3.Text(stringResource(R.string.form_label_247198))})
                        state.categories.forEach { category->androidx.compose.material3.FilterChip(selectedCategory==category.id,{selectedCategory=category.id},label={androidx.compose.material3.Text(category.name)}) }
                        androidx.compose.material3.OutlinedTextField(filterTags,{filterTags=it},label={androidx.compose.material3.Text(stringResource(R.string.form_label_d92fa5))})
                    }
                },confirmButton={androidx.compose.material3.TextButton(onClick={onApplyFilters(selectedCategory,filterTags);showFilters=false}) {androidx.compose.material3.Text(stringResource(R.string.form_label_387b53))}},dismissButton={androidx.compose.material3.TextButton(onClick={showFilters=false}) {androidx.compose.material3.Text(stringResource(R.string.form_label_8b4c20))}})
                if (state.loading || styleStatus == MapStyleStatus.LOADING || locating) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                if (styleStatus == MapStyleStatus.ERROR) {
                    MapNotice("Could not load the map.", "Retry map") { styleAttempt++ }
                }
                state.error?.let { MapNotice(it, "Retry events", onRetry) }
                if (state.loaded && !state.loading && state.error == null && styleStatus == MapStyleStatus.READY &&
                    state.pins.isEmpty() && state.clusters.isEmpty()
                ) {
                    MapNotice(if (state.query.isBlank()) "No events in this area." else "No matching events.")
                }
                if (locating) MapNotice("Finding your location…")
                if (locationNotice != null) {
                    MapNotice(locationNotice, if (onLocationSettings == null) "Dismiss" else stringResource(R.string.form_settings_c7f73b),
                        onLocationSettings ?: onDismissLocationNotice)
                }
            }
            Column(
                modifier = Modifier.align(Alignment.BottomEnd).padding(start = Spacing.m, end = Spacing.m, bottom = 38.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                MapControls(onZoomIn, onZoomOut, onRecenter, onMyLocation.takeUnless { locating })
                state.selected?.let { MapEventPreviewCard(it, onOpenEvent, onDismissPreview) }
                if (state.detailsLoading) MapNotice("Loading event…", "Dismiss", onDismissPreview)
                state.detailsError?.let { message ->
                    MapNotice(message, "Retry event") { state.selectedPin?.let(onPinChoice) }
                }
            }
            MapAttribution(Modifier.align(Alignment.BottomStart).padding(horizontal = Spacing.s, vertical = Spacing.xs))
        }
    }

    if (state.pinChoices.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = onDismissPreview,
            title = { Text(stringResource(R.string.form_events_at_this_location_0a4067)) },
            text = {
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    state.pinChoices.forEach { pin ->
                        TextButton(onClick = { onPinChoice(pin) }) { Text(pin.eventTitle ?: "Event ${pin.id}") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = onDismissPreview) { Text(stringResource(R.string.form_close_bbfa77)) } },
        )
    }
}

@Composable
private fun MapNotice(message: String, actionLabel: String? = null, action: () -> Unit = {}) {
    Surface(shape = MaterialTheme.shapes.small, tonalElevation = 2.dp) {
        Row(Modifier.padding(horizontal = Spacing.s, vertical = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
            Text(message, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            if (actionLabel != null) TextButton(onClick = action) { Text(actionLabel) }
        }
    }
}

@Composable
internal fun MapAttribution(modifier: Modifier) {
    val linkStyle = TextLinkStyles(style = SpanStyle(textDecoration = TextDecoration.Underline))
    Surface(modifier, shape = MaterialTheme.shapes.extraSmall) {
        Text(
            buildAnnotatedString {
                withLink(LinkAnnotation.Url("https://openfreemap.org/", linkStyle)) { append("OpenFreeMap") }
                append(" · © ")
                withLink(LinkAnnotation.Url("https://openmaptiles.org/", linkStyle)) { append("OpenMapTiles") }
                append(" · © ")
                withLink(LinkAnnotation.Url("https://www.openstreetmap.org/copyright", linkStyle)) { append("OpenStreetMap") }
            },
            modifier = Modifier.padding(horizontal = Spacing.s, vertical = Spacing.xs),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
