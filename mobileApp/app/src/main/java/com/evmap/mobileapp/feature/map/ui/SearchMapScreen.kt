package feature.map.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import core.designsystem.AppTheme
import core.designsystem.Spacing
import core.ui.components.AppBottomBar
import core.ui.components.SearchField
import core.ui.model.EventUi
import core.ui.model.MapCameraUi
import core.ui.model.MapPinUi
import feature.map.ui.components.MapControls
import feature.map.ui.components.MapEventPreviewCard

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun SearchMapScreenPreview() {
    val samplePins = listOf(
        MapPinUi(id = "p1", lat = 52.2297, lng = 21.0122, eventTitle = "Pin 1"),
        MapPinUi(id = "p2", lat = 52.23,   lng = 21.01,   eventTitle = "Pin 2")
    )
    val sampleCamera = MapCameraUi(
        lat = 52.2297,
        lng = 21.0122,
        zoom = 12f
    )
    val sampleEvent = EventUi(
        id = "e1",
        title = "Sample Event",
        location = "Warsaw",
        startsAt = "startat",
        description = "A short description used only for preview.",
        imageUrl = null,
        rating = 4.7F,
        reviewCount = 123
    )

    SearchMapScreen(
        query = "Coffee",
        pins = samplePins,
        camera = sampleCamera,
        selected = sampleEvent,
        onQueryChange = {},
        onPinClick = {},
        onRecenter = {},
        onZoomIn = {},
        onZoomOut = {},
        onMyLocation = {},
        onOpenEvent = {},
        onDismissPreview = {},
        currentRoute = "map",
        onNavigate = {},
        modifier = Modifier.fillMaxSize()
    )
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchMapScreen(
    query: String,
    pins: List<MapPinUi>,
    camera: MapCameraUi,
    selected: EventUi?,
    onQueryChange: (String) -> Unit,
    onPinClick: (String) -> Unit,
    onRecenter: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onMyLocation: (() -> Unit)?,
    onOpenEvent: (String) -> Unit,
    onDismissPreview: () -> Unit,
    currentRoute: String? = "map",
    onNavigate: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    AppTheme {
        Scaffold(
            bottomBar = {
                AppBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = onNavigate
                )
            },
            containerColor = MaterialTheme.colorScheme.surface
        ) { paddingValues ->
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Main Map Area (Placeholder for Google Maps Compose)
                // TODO: Replace with Google Maps Compose when available
                MapPlaceholder(
                    pins = pins,
                    camera = camera,
                    onPinClick = onPinClick,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Search Field Overlay (Top)
                SearchField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = "Hinted search text",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(horizontal = Spacing.l, vertical = Spacing.s)
                )
                
                // Map Controls Overlay (Bottom Right)
                MapControls(
                    onZoomIn = onZoomIn,
                    onZoomOut = onZoomOut,
                    onRecenter = onRecenter,
                    onMyLocation = onMyLocation,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = Spacing.m, bottom = Spacing.xl)
                )
                
                // Event Preview Card Overlay (Bottom Center)
                selected?.let { event ->
                    MapEventPreviewCard(
                        event = event,
                        onOpenEvent = onOpenEvent,
                        onDismiss = onDismissPreview,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = Spacing.xxl)
                    )
                }
            }
        }
    }
}

@Composable
private fun MapPlaceholder(
    pins: List<MapPinUi>,
    camera: MapCameraUi,
    onPinClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Placeholder implementation for Google Maps Compose
    // This displays a simple colored background with pin count and camera info
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF0F0F0)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.s)
        ) {
            Text(
                text = "Map Placeholder",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Pins: ${pins.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Camera: (${camera.lat}, ${camera.lng}) Zoom: ${camera.zoom}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Simulate pin markers
            if (pins.isNotEmpty()) {
                Text(
                    text = "Tap here to simulate pin click",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Button(
                    onClick = { 
                        // Simulate clicking the first pin
                        if (pins.isNotEmpty()) {
                            onPinClick(pins.first().id)
                        }
                    }
                ) {
                    Text("Click First Pin")
                }
            }
        }
    }
}
