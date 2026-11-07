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
import com.quickgit.app.ui.theme.GitGreen

/**
 * Single top-bar action/back control as a floating circular chip.
 * Green highlight matches the selected floating navigation item treatment.
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
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
        color = GitGreen.copy(alpha = 0.2f),
        modifier = modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = GitGreen
            )
        }
    }
}
