package com.quickgit.app.ui.components

import android.content.Context
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickgit.app.ui.theme.GitGreen
import kotlin.math.sqrt

data class FloatingNavItem(
    val icon: ImageVector,
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit
)

/** Physical screen diagonal in inches (best-effort from DisplayMetrics). */
fun Context.screenDiagonalInches(): Float {
    val m = resources.displayMetrics
    val w = m.widthPixels / m.xdpi.toDouble()
    val h = m.heightPixels / m.ydpi.toDouble()
    return sqrt(w * w + h * h).toFloat()
}

/**
 * Material 3 floating bottom navigation (phones ≥ 6.3″ and tablets).
 *
 * All destinations share equal width in one row so every option stays visible
 * without horizontal scrolling. Chromebooks use the side rail instead.
 */
@Composable
fun FloatingTabletNavBar(
    items: List<FloatingNavItem>,
    modifier: Modifier = Modifier,
    /** Tighter chrome for phone-width layouts so every item still fits. */
    compact: Boolean = false
) {
    val navPad = WindowInsets.navigationBars.asPaddingValues()
    val shape = RoundedCornerShape(if (compact) 24.dp else 28.dp)
    val hPad = if (compact) 10.dp else 16.dp
    val vPad = if (compact) 8.dp else 12.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(navPad)
            .padding(horizontal = hPad, vertical = vPad),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .widthIn(max = if (compact) 560.dp else 720.dp)
                .fillMaxWidth()
                .clip(shape)
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Box(
                    Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f))
                        .blur(20.dp)
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
                        .fillMaxWidth()
                        .padding(
                            horizontal = if (compact) 4.dp else 8.dp,
                            vertical = if (compact) 6.dp else 8.dp
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEach { item ->
                        FloatingNavBarItem(
                            item = item,
                            compact = compact,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingNavBarItem(
    item: FloatingNavItem,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val selectedBg = if (item.selected) GitGreen.copy(alpha = 0.18f) else Color.Transparent
    val contentColor = if (item.selected) GitGreen else MaterialTheme.colorScheme.onSurfaceVariant
    // Shorter labels so 7–8 destinations fit on phone widths.
    val shortLabel = when (item.label) {
        "Repos" -> "Repos"
        "Clone" -> "Clone"
        "Browse" -> "Browse"
        "Profile" -> "Profile"
        "Users" -> "Users"
        "Logs" -> "Logs"
        "Creds" -> "Creds"
        "Settings" -> "Settings"
        else -> item.label.take(6)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(selectedBg)
            .clickable(
                interactionSource = interaction,
                indication = rememberRipple(bounded = true, color = GitGreen),
                onClick = item.onClick
            )
            .padding(vertical = if (compact) 4.dp else 6.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            item.icon,
            contentDescription = item.label,
            tint = contentColor,
            modifier = Modifier.size(if (compact) 20.dp else 22.dp)
        )
        Text(
            shortLabel,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (item.selected) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = if (compact) 9.sp else 10.sp,
                lineHeight = if (compact) 11.sp else 12.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Clip,
            textAlign = TextAlign.Center
        )
    }
}
