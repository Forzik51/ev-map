package feature.feed

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.evmap.mobileapp.feature.feed.ui.FeedRoute
import core.navigation.FeatureEntry

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
                onOpenSearch = { /* open search */ },
                onOpenCreate = { onNavigateToRoute("events/create") },
                onNavigate = onNavigateToRoute
            )
        }
    }
}