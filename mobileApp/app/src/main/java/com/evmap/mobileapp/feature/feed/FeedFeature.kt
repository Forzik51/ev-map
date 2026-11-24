package feature.feed

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.evmap.mobileapp.feature.feed.ui.FeedRoute
import core.navigation.FeatureEntry
import feature.events.EventsFeature
import feature.explore.ExploreFeature
import feature.saved.SavedFeature

object FeedFeature : FeatureEntry {
    override val route = "feed"

    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        onNavigateToRoute: (String) -> Unit,
        onNavigateUp: () -> Unit
    ) {
        navGraphBuilder.composable(route) {
            FeedRoute(
                currentRoute = route,
                onOpenItem = { eventId -> onNavigateToRoute("events/$eventId") },
                onSaveItem = { onNavigateToRoute(SavedFeature.pickerRoute(it)) },
                onOpenSearch = { query -> onNavigateToRoute(ExploreFeature.routeForQuery(query)) },
                onOpenCatalog = { onNavigateToRoute(EventsFeature.List) },
                onNavigate = onNavigateToRoute
            )
        }
    }
}
