package feature.map.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import core.designsystem.Spacing
import core.ui.components.ActionIconButton

@Composable
fun MapControls(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onRecenter: () -> Unit,
    onMyLocation: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
        horizontalAlignment = Alignment.End
    ) {
        // Zoom In
        ActionIconButton(
            icon = Icons.Default.Add,
            contentDescription = "Zoom In",
            onClick = onZoomIn
        )
        
        // Zoom Out
        ActionIconButton(
            icon = Icons.Default.Remove,
            contentDescription = "Zoom Out",
            onClick = onZoomOut
        )
        
        Spacer(modifier = Modifier.height(Spacing.s))
        
        // Recenter
        ActionIconButton(
            icon = Icons.Default.CenterFocusStrong,
            contentDescription = "Recenter",
            onClick = onRecenter
        )
        
        // My Location (optional)
        onMyLocation?.let { onMyLocationClick ->
            ActionIconButton(
                icon = Icons.Default.MyLocation,
                contentDescription = "My Location",
                onClick = onMyLocationClick
            )
        }
    }
}
