package eu.dotshell.pelo.generic.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Unified shadow system for the Pelo app
 * Provides consistent shadows across all components
 */

// Shadow elevation levels (in dp)
val ShadowElevation = object {
    val none: Dp = 0.dp
    val small: Dp = 2.dp
    val medium: Dp = 4.dp
    val large: Dp = 8.dp
    val xlarge: Dp = 12.dp
}

/**
 * Standard shadow alpha values based on elevation
 */
private fun getShadowAlpha(elevation: Dp): Float {
    return when {
        elevation <= ShadowElevation.small -> 0.12f
        elevation <= ShadowElevation.medium -> 0.16f
        elevation <= ShadowElevation.large -> 0.20f
        else -> 0.24f
    }
}

/**
 * Standard button elevation modifier
 * Applies consistent elevation and shadow to buttons
 */
fun Modifier.buttonElevation(
    elevation: Dp = ShadowElevation.medium
): Modifier = this.graphicsLayer {
    shadowElevation = elevation
    shadowColor = Color.Black.copy(alpha = getShadowAlpha(elevation))
    shape = RectangleShape
    clip = false
}

/**
 * Standard card elevation modifier
 * Applies consistent elevation and shadow to cards
 */
fun Modifier.cardElevation(
    elevation: Dp = ShadowElevation.medium
): Modifier = this.graphicsLayer {
    shadowElevation = elevation
    shadowColor = Color.Black.copy(alpha = getShadowAlpha(elevation))
    shape = RectangleShape
    clip = false
}

/**
 * Floating action button elevation
 * Higher elevation for FABs
 */
fun Modifier.fabElevation(): Modifier = this.graphicsLayer {
    shadowElevation = ShadowElevation.xlarge
    shadowColor = Color.Black.copy(alpha = getShadowAlpha(ShadowElevation.xlarge))
    shape = RectangleShape
    clip = false
}

/**
 * Standard elevated container
 * For surfaces that need to appear raised
 */
@Composable
fun ElevatedSurface(
    elevation: Dp = ShadowElevation.medium,
    shape: Shape = RectangleShape,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .graphicsLayer {
                this.shadowElevation = elevation
                this.shadowColor = Color.Black.copy(alpha = getShadowAlpha(elevation))
                this.shape = shape
                this.clip = false
            }
    ) {
        content()
    }
}

/**
 * Standard shadow for icons and small elements
 */
fun Modifier.iconShadow(): Modifier = this.graphicsLayer {
    shadowElevation = ShadowElevation.small
    shadowColor = Color.Black.copy(alpha = getShadowAlpha(ShadowElevation.small))
    shape = RectangleShape
    clip = false
}

/**
 * Extension to add elevation to any modifier
 */
fun Modifier.withElevation(
    elevation: Dp
): Modifier = this.graphicsLayer {
    shadowElevation = elevation
    shadowColor = Color.Black.copy(alpha = getShadowAlpha(elevation))
    shape = RectangleShape
    clip = false
}
