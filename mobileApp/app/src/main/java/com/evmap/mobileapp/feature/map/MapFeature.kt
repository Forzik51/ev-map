package feature.map

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import core.navigation.FeatureEntry
import feature.map.ui.SearchMapScreen

object MapFeature : FeatureEntry {
    override val route = "map"
    
    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        onNavigateToRoute: (String) -> Unit,
        onNavigateUp: () -> Unit
    ) {
        navGraphBuilder.composable(route = route) {
            // TODO: Replace with actual state management/ViewModel
            SearchMapScreen(
                query = "",
                pins = emptyList(),
                camera = core.ui.model.MapCameraUi(lat = 37.7749, lng = -122.4194, zoom = 10f),
                selected = null,
                onQueryChange = { /* TODO: Handle query change */ },
                onPinClick = { /* TODO: Handle pin click */ },
                onRecenter = { /* TODO: Handle recenter */ },
                onZoomIn = { /* TODO: Handle zoom in */ },
                onZoomOut = { /* TODO: Handle zoom out */ },
                onMyLocation = { /* TODO: Handle my location */ },
                onOpenEvent = { eventId ->
                    // TODO: Navigate to event details
                    onNavigateToRoute("event/$eventId")
                },
                onDismissPreview = { /* TODO: Handle dismiss preview */ },
                currentRoute = route,
                onNavigate = onNavigateToRoute
            )
        }
    }
}
