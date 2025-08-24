package core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import core.designsystem.Spacing

@Composable
fun RatingRow(
    rating: Float,
    modifier: Modifier = Modifier,
    maxStars: Int = 5,
    starSize: Int = 20
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(maxStars) { index ->
            val isFilled = index < rating.toInt()
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = if (isFilled) "Filled star" else "Empty star",
                modifier = Modifier.size(starSize.dp),
                tint = if (isFilled) Color(0xFF625B71) else Color(0xFFCAC4D0)
            )
        }
    }
}
