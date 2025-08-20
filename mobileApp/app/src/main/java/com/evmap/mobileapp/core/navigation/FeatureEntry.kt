package core.navigation

import androidx.navigation.NavGraphBuilder

interface FeatureEntry {
    val route: String
    
    fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        onNavigateToRoute: (String) -> Unit,
        onNavigateUp: () -> Unit
    )
}
