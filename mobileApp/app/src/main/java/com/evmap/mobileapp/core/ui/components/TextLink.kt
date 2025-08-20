package core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration

@Composable
fun TextLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val textDecoration = if (isHovered || isPressed) {
        TextDecoration.Underline
    } else {
        TextDecoration.None
    }
    
    Text(
        text = text,
        modifier = modifier.clickable(
            interactionSource = interactionSource,
            indication = null
        ) { onClick() },
        style = MaterialTheme.typography.labelMedium.copy(
            textDecoration = textDecoration
        ),
        color = MaterialTheme.colorScheme.primary
    )
}
