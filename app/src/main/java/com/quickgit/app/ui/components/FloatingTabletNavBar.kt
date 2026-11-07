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
import kotlin.math.max
import kotlin.math.sqrt

data class FloatingNavItem(
    val icon: ImageVector,
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit
)

/** Physical screen diagonal in inches (best-effort). */
fun Context.screenDiagonalInches(): Float {
    val m = resources.displayMetrics
    val xdpi = m.xdpi.takeIf { it > 0f } ?: m.densityDpi.toFloat()
    val ydpi = m.ydpi.takeIf { it > 0f } ?: m.densityDpi.toFloat()
    val w = m.widthPixels / xdpi.toDouble()
    val h = m.heightPixels / ydpi.toDouble()
    return sqrt(w * w + h * h).toFloat()
}

/**
 * True for phone-class devices that should use the floating bottom navbar:
 * physical diagonal > 6.3″, or dp heuristics when DPI reporting is wrong.
 * Excludes tablets (smallest width ≥ 600dp).
 */
fun Context.isLargePhoneForFloatingNav(): Boolean {
    val cfg = resources.configuration
    val sw = cfg.smallestScreenWidthDp
    if (sw >= 600) return false
    val longDp = max(cfg.screenWidthDp, cfg.screenHeightDp)
    val shortDp = minOf(cfg.screenWidthDp, cfg.screenHeightDp)
    val diag = screenDiagonalInches()
    return diag > 6.3f ||
        (longDp >= 780 && shortDp < 600) ||
        (sw >= 392 && longDp >= 740)
}

/**
 * Material 3 floating bottom navigation for large phones and tablets.
 * Equal-weight items so every destination stays on-screen without scrolling.
 */
@Composable
fun FloatingTabletNavBar(
    items: List<FloatingNavItem>,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val navPad = WindowInsets.navigationBars.asPaddingValues()
    val shape = RoundedCornerShape(if (compact) 22.dp else 28.dp)
    val hPad = if (compact) 28.dp else 16.dp
    val vPad = if (compact) 6.dp else 12.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(navPad)
            .padding(horizontal = hPad, vertical = vPad),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .widthIn(max = if (compact) 400.dp else 720.dp)
                .then(if (compact) Modifier.fillMaxWidth(0.88f) else Modifier.fillMaxWidth())
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
                    alpha = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.78f else 0.96f
                ),
                tonalElevation = 4.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = if (compact) 2.dp else 6.dp,
                            vertical = if (compact) 5.dp else 8.dp
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
    // Match FloatingCircleIconButton fill so selected tabs share the lighter chip look.
    val selectedBg = if (item.selected) {
        MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.92f)
    } else {
        Color.Transparent
    }
    val contentColor = if (item.selected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
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
            .clip(RoundedCornerShape(12.dp))
            .background(selectedBg)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = item.onClick
            )
            .padding(vertical = if (compact) 3.dp else 5.dp, horizontal = 1.dp),
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
                lineHeight = 11.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Clip,
            textAlign = TextAlign.Center
        )
    }
}
