package com.quickgit.app.ui.components

import com.quickgit.app.ui.theme.GitGreen

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
    /** Always match [RepoDetailBottomBar] chrome (icons 20.dp, labelSmall). */
    compact: Boolean = true
) {
    val navPad = WindowInsets.navigationBars.asPaddingValues()
    // Same outer chrome as RepoDetailBottomBar on phones and tablets
    val shape = RoundedCornerShape(22.dp)
    val hPad = 28.dp
    val vPad = 6.dp
    // Wider max when many destinations (Browse/Creds/etc. on tablets)
    val maxBarWidth = if (items.size >= 6) 560.dp else 460.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(navPad)
            .padding(horizontal = hPad, vertical = vPad),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .widthIn(max = maxBarWidth)
                .fillMaxWidth(0.94f)
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
                        .padding(horizontal = 2.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEach { item ->
                        FloatingNavBarItem(
                            item = item,
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
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val selectedBg = if (item.selected) GitGreen.copy(alpha = 0.2f) else Color.Transparent
    val contentColor = if (item.selected) GitGreen else MaterialTheme.colorScheme.onSurfaceVariant
    val shortLabel = when (item.label) {
        "Repos" -> "Repos"
        "Clone" -> "Clone"
        "Browse" -> "Browse"
        "Profiles" -> "Profiles"
        "Users" -> "Users"
        "Search" -> "Search"
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
            // Match RepoDetailBottomBar item padding exactly
            .padding(vertical = 3.dp, horizontal = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            item.icon,
            contentDescription = item.label,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        Text(
            shortLabel,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            textAlign = TextAlign.Center,
            fontWeight = if (item.selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}
