package com.quickgit.app.ui.components

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import com.quickgit.app.ui.theme.GitGreen

data class FloatingNavItem(
    val icon: ImageVector,
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit
)

/**
 * Material 3–style floating bottom navigation for Android tablets.
 *
 * Uses a translucent elevated pill with a soft blur layer (API 31+) so content
 * behind the bar reads as frosted glass. Chromebooks keep the side [NavigationRail]
 * instead of this bar.
 */
@Composable
fun FloatingTabletNavBar(
    items: List<FloatingNavItem>,
    modifier: Modifier = Modifier
) {
    val navPad = WindowInsets.navigationBars.asPaddingValues()
    val shape = RoundedCornerShape(28.dp)
    val scroll = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(navPad)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Soft frosted backdrop: translucent surface + blur of the bar's own glass layer.
        Box(
            Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .clip(shape)
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Box(
                    Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f))
                        .blur(24.dp)
                )
            }
            Surface(
                shape = shape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(
                    alpha = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.72f else 0.94f
                ),
                tonalElevation = 3.dp,
                shadowElevation = 10.dp,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier
                        .horizontalScroll(scroll)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEach { item ->
                        FloatingNavBarItem(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingNavBarItem(item: FloatingNavItem) {
    val interaction = remember { MutableInteractionSource() }
    val selectedBg = if (item.selected) {
        GitGreen.copy(alpha = 0.18f)
    } else {
        Color.Transparent
    }
    val contentColor = if (item.selected) {
        GitGreen
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(selectedBg)
            .clickable(
                interactionSource = interaction,
                indication = rememberRipple(bounded = true, color = GitGreen),
                onClick = item.onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .widthIn(min = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            item.icon,
            contentDescription = item.label,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.size(2.dp))
        Text(
            item.label,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (item.selected) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 11.sp
            ),
            maxLines = 1
        )
    }
}
