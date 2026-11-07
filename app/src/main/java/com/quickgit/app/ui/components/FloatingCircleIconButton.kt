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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Single top-bar action/back control as a floating circular chip.
 *
 * Filled lighter than the title bar / floating nav (`surfaceContainerHigh`)
 * by lerping toward white so chips never read as black in dark theme.
 */
@Composable
fun FloatingCircleIconButton(
    imageVector: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val chipColor = lerp(
        MaterialTheme.colorScheme.surfaceContainerHigh,
        Color.White,
        0.55f
    ).copy(alpha = 0.95f)

    Surface(
        onClick = onClick,
        shape = CircleShape,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
        color = chipColor,
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
