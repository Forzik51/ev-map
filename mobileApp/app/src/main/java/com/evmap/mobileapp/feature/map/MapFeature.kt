package feature.map

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.evmap.mobileapp.feature.map.ui.MapViewModel
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
            val viewModel: MapViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()

            SearchMapScreen(
                query = state.query,
                pins = state.pins,
                camera = state.camera,
                selected = state.selected,
                onQueryChange = viewModel::updateQuery,
                onPinClick = viewModel::selectPin,
                onRecenter = viewModel::loadNearby,
                onZoomIn = viewModel::zoomIn,
                onZoomOut = viewModel::zoomOut,
                onMyLocation = viewModel::loadNearby,
                onOpenEvent = { eventId ->
                    onNavigateToRoute("events/$eventId")
                },
                onDismissPreview = viewModel::dismissPreview,
                currentRoute = route,
                onNavigate = onNavigateToRoute
            )
        }
    }
}
