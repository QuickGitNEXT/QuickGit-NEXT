package com.quickgit.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Single top-bar action/back control as a floating circular chip.
 * Used when a screen only has a corner icon (no dense action row).
 *
 * Filled lighter than the title bar / floating nav (`surfaceContainerHigh`)
 * so the circles read as secondary chrome on top of those surfaces.
 */
@Composable
fun FloatingCircleIconButton(
    imageVector: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        tonalElevation = 1.dp,
        shadowElevation = 3.dp,
        // Lighter than title/nav bars (surfaceContainerHigh ~0.96)
        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.92f),
        modifier = modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
